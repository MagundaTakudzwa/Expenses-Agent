-- V3: Purchase requests (asking permission BEFORE buying) and their item rows.

CREATE TABLE purchase_requests (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id      BIGINT         NOT NULL,
    requested_by_id BIGINT         NOT NULL,
    category_id     BIGINT,
    quote_source    VARCHAR(20)    NOT NULL,
    purpose         VARCHAR(255)   NOT NULL,
    supplier_name   VARCHAR(150),
    currency        VARCHAR(3)     NOT NULL,
    estimated_total NUMERIC(14, 2) NOT NULL,
    notes           VARCHAR(1000),
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    decided_by_id   BIGINT,
    decided_at      TIMESTAMPTZ,
    decision_note   VARCHAR(500),
    version         BIGINT         NOT NULL DEFAULT 0,
    submitted_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- Each person and category must belong to the SAME company as the request.
    CONSTRAINT fk_purchase_requests_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_purchase_requests_requested_by
        FOREIGN KEY (company_id, requested_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_purchase_requests_decided_by
        FOREIGN KEY (company_id, decided_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_purchase_requests_category
        FOREIGN KEY (company_id, category_id) REFERENCES expense_categories (company_id, id),

    CONSTRAINT ck_purchase_requests_quote_source
        CHECK (quote_source IN ('SUPPLIER_QUOTE', 'WORKER_ESTIMATE')),
    CONSTRAINT ck_purchase_requests_currency
        CHECK (currency IN ('USD', 'ZWG', 'ZAR')),
    CONSTRAINT ck_purchase_requests_estimated_total
        CHECK (estimated_total > 0),
    CONSTRAINT ck_purchase_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    -- Nobody may decide on their own request.
    CONSTRAINT ck_purchase_requests_not_self_decided
        CHECK (decided_by_id IS NULL OR decided_by_id <> requested_by_id),
    -- Who decided and when are always recorded together.
    CONSTRAINT ck_purchase_requests_decision_pair
        CHECK ((decided_by_id IS NULL) = (decided_at IS NULL)),
    -- Pending requests have no decision; approved, rejected and completed ones must have one.
    CONSTRAINT ck_purchase_requests_decision_matches_status
        CHECK (
            (status = 'PENDING' AND decided_by_id IS NULL)
            OR (status IN ('APPROVED', 'REJECTED', 'COMPLETED') AND decided_by_id IS NOT NULL)
            OR status = 'CANCELLED'
        ),
    -- A rejection must give the worker a reason.
    CONSTRAINT ck_purchase_requests_rejection_reason
        CHECK (status <> 'REJECTED' OR decision_note IS NOT NULL),

    CONSTRAINT uq_purchase_requests_company_id_id UNIQUE (company_id, id)
);

-- Speeds up "pending requests in my company, newest first" (the manager's list).
CREATE INDEX ix_purchase_requests_company_status
    ON purchase_requests (company_id, status, submitted_at DESC);

-- Speeds up "my requests, newest first" (the worker's list).
CREATE INDEX ix_purchase_requests_requested_by
    ON purchase_requests (company_id, requested_by_id, submitted_at DESC);

CREATE TABLE purchase_request_items (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id           BIGINT         NOT NULL,
    purchase_request_id  BIGINT         NOT NULL,
    line_number          INTEGER        NOT NULL,
    description          VARCHAR(200)   NOT NULL,
    normalized_name      VARCHAR(200),
    quantity             NUMERIC(12, 3) NOT NULL,
    unit                 VARCHAR(10)    NOT NULL DEFAULT 'EACH',
    estimated_unit_price NUMERIC(12, 2) NOT NULL,
    -- Calculated by the database itself, so it can never disagree with quantity x price.
    line_total           NUMERIC(14, 2)
        GENERATED ALWAYS AS (round(quantity * estimated_unit_price, 2)) STORED,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_purchase_request_items_request
        FOREIGN KEY (company_id, purchase_request_id) REFERENCES purchase_requests (company_id, id),
    CONSTRAINT ck_purchase_request_items_line_number
        CHECK (line_number > 0),
    CONSTRAINT ck_purchase_request_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_purchase_request_items_unit
        CHECK (unit IN ('EACH', 'LITRE', 'KG', 'METRE', 'PACK', 'BAG', 'BOX')),
    CONSTRAINT ck_purchase_request_items_unit_price
        CHECK (estimated_unit_price >= 0),
    CONSTRAINT uq_purchase_request_items_line
        UNIQUE (purchase_request_id, line_number),
    CONSTRAINT uq_purchase_request_items_company_id_id
        UNIQUE (company_id, id)
);