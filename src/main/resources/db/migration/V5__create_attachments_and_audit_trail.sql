-- V5: Attachments (receipt photos, quotes) and the permanent audit trail.

CREATE TABLE attachments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id          BIGINT       NOT NULL,
    uploaded_by_id      BIGINT       NOT NULL,
    expense_id          BIGINT,
    purchase_request_id BIGINT,
    kind                VARCHAR(20)  NOT NULL,
    storage_key         VARCHAR(500) NOT NULL,
    original_filename   VARCHAR(255),
    content_type        VARCHAR(100) NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    sha256              CHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_attachments_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_attachments_uploaded_by
        FOREIGN KEY (company_id, uploaded_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_attachments_expense
        FOREIGN KEY (company_id, expense_id) REFERENCES expenses (company_id, id),
    CONSTRAINT fk_attachments_purchase_request
        FOREIGN KEY (company_id, purchase_request_id) REFERENCES purchase_requests (company_id, id),

    -- Every file belongs to exactly one expense OR one purchase request.
    CONSTRAINT ck_attachments_one_owner
        CHECK (num_nonnulls(expense_id, purchase_request_id) = 1),
    CONSTRAINT ck_attachments_kind
        CHECK (kind IN ('RECEIPT', 'QUOTE', 'PUMP_DISPLAY', 'ODOMETER', 'OTHER')),
    CONSTRAINT ck_attachments_content_type
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf')),
    -- Files up to 20 MB.
    CONSTRAINT ck_attachments_size
        CHECK (size_bytes > 0 AND size_bytes <= 20971520),
    -- A SHA-256 fingerprint is exactly 64 lowercase hexadecimal characters.
    CONSTRAINT ck_attachments_sha256
        CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT uq_attachments_storage_key UNIQUE (storage_key)
);

CREATE INDEX ix_attachments_expense
    ON attachments (company_id, expense_id)
    WHERE expense_id IS NOT NULL;

CREATE INDEX ix_attachments_purchase_request
    ON attachments (company_id, purchase_request_id)
    WHERE purchase_request_id IS NOT NULL;

-- Spotting the same photo uploaded twice within a company.
CREATE INDEX ix_attachments_fingerprint
    ON attachments (company_id, sha256);

CREATE TABLE audit_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id  BIGINT      NOT NULL,
    actor_id    BIGINT,
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id   BIGINT      NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20),
    details     JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_events_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    -- An empty actor means the system did it (for example, AI reading a receipt).
    CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (company_id, actor_id) REFERENCES app_users (company_id, id),
    CONSTRAINT ck_audit_events_entity_type
        CHECK (entity_type IN ('COMPANY', 'USER', 'EXPENSE_CATEGORY', 'FUEL_TYPE',
                               'PURCHASE_REQUEST', 'EXPENSE', 'ATTACHMENT'))
);

-- "Show the full history of this expense", oldest first.
CREATE INDEX ix_audit_events_entity
    ON audit_events (company_id, entity_type, entity_id, occurred_at);

-- "Show everything that happened in my company recently".
CREATE INDEX ix_audit_events_company_time
    ON audit_events (company_id, occurred_at DESC);

-- The audit trail is append-only: rows can be added, never changed or removed.
CREATE FUNCTION prevent_audit_event_changes() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only: % is not allowed', TG_OP;
END;
$$;

CREATE TRIGGER trg_audit_events_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_changes();

CREATE TRIGGER trg_audit_events_no_truncate
    BEFORE TRUNCATE ON audit_events
    FOR EACH STATEMENT EXECUTE FUNCTION prevent_audit_event_changes();