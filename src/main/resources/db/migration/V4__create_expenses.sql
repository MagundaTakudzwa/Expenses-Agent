-- V4: Expense claims (after the money is spent) and their item rows.

CREATE TABLE expenses (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id              BIGINT         NOT NULL,
    submitted_by_id         BIGINT         NOT NULL,
    category_id             BIGINT         NOT NULL,
    claim_type              VARCHAR(20)    NOT NULL,
    purchase_request_id     BIGINT,
    skip_approval_reason    VARCHAR(500),

    -- What was bought, where, when, and how much
    description             VARCHAR(255)   NOT NULL,
    supplier_name           VARCHAR(150),
    expense_date            DATE           NOT NULL,
    currency                VARCHAR(3)     NOT NULL,
    amount                  NUMERIC(14, 2) NOT NULL,
    paid_with               VARCHAR(20)    NOT NULL,
    payment_method          VARCHAR(20),
    fiscal_invoice_number   VARCHAR(50),

    -- Fuel claims
    fuel_type_id            BIGINT,
    fuel_litres             NUMERIC(10, 3),
    fuel_price_per_litre    NUMERIC(10, 4),
    vehicle_registration    VARCHAR(20),
    odometer_km             INTEGER,

    -- Mileage claims (own vehicle)
    trip_from               VARCHAR(150),
    trip_to                 VARCHAR(150),
    distance_km             NUMERIC(10, 1),
    mileage_rate            NUMERIC(10, 4),

    -- Subsistence (per diem) claims
    trip_location           VARCHAR(150),
    subsistence_days        NUMERIC(5, 1),
    daily_rate              NUMERIC(12, 2),

    -- Receipt reading
    extraction_status       VARCHAR(20)    NOT NULL DEFAULT 'NOT_APPLICABLE',
    extracted_data          JSONB,

    -- Review by a manager
    status                  VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_id          BIGINT,
    reviewed_at             TIMESTAMPTZ,
    review_note             VARCHAR(500),

    -- Paying the worker back
    reimbursed_by_id        BIGINT,
    reimbursed_at           TIMESTAMPTZ,
    reimbursement_reference VARCHAR(100),

    version                 BIGINT         NOT NULL DEFAULT 0,
    submitted_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- Everything linked must belong to the SAME company as the expense.
    CONSTRAINT fk_expenses_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_expenses_submitted_by
        FOREIGN KEY (company_id, submitted_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_expenses_reviewed_by
        FOREIGN KEY (company_id, reviewed_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_expenses_reimbursed_by
        FOREIGN KEY (company_id, reimbursed_by_id) REFERENCES app_users (company_id, id),
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (company_id, category_id) REFERENCES expense_categories (company_id, id),
    CONSTRAINT fk_expenses_purchase_request
        FOREIGN KEY (company_id, purchase_request_id) REFERENCES purchase_requests (company_id, id),
    CONSTRAINT fk_expenses_fuel_type
        FOREIGN KEY (company_id, fuel_type_id) REFERENCES fuel_types (company_id, id),

    -- Allowed values
    CONSTRAINT ck_expenses_claim_type
        CHECK (claim_type IN ('RECEIPT', 'FUEL', 'MILEAGE', 'SUBSISTENCE')),
    CONSTRAINT ck_expenses_currency
        CHECK (currency IN ('USD', 'ZWG', 'ZAR')),
    CONSTRAINT ck_expenses_amount
        CHECK (amount > 0),
    CONSTRAINT ck_expenses_paid_with
        CHECK (paid_with IN ('WORKER_OWN_MONEY', 'COMPANY_ADVANCE', 'COMPANY_CARD', 'FUEL_COUPON')),
    CONSTRAINT ck_expenses_payment_method
        CHECK (payment_method IS NULL OR payment_method IN
               ('CASH', 'ECOCASH', 'ONEMONEY', 'INNBUCKS', 'CARD_SWIPE', 'BANK_TRANSFER')),
    CONSTRAINT ck_expenses_extraction_status
        CHECK (extraction_status IN ('NOT_APPLICABLE', 'PENDING', 'EXTRACTED', 'LOW_CONFIDENCE', 'UNREADABLE')),
    CONSTRAINT ck_expenses_status
        CHECK (status IN ('SUBMITTED', 'VERIFIED', 'QUERIED', 'REJECTED', 'REIMBURSED')),

    -- A claim either links to an approved request, or explains why it skipped approval.
    CONSTRAINT ck_expenses_skip_reason_only_without_request
        CHECK (purchase_request_id IS NULL OR skip_approval_reason IS NULL),

    -- Fuel, mileage and subsistence claims must carry their own details.
    CONSTRAINT ck_expenses_fuel_details
        CHECK (claim_type <> 'FUEL'
               OR (fuel_type_id IS NOT NULL AND fuel_litres > 0 AND fuel_price_per_litre > 0)),
    CONSTRAINT ck_expenses_odometer
        CHECK (odometer_km IS NULL OR odometer_km >= 0),
    CONSTRAINT ck_expenses_mileage_details
        CHECK (claim_type <> 'MILEAGE'
               OR (trip_from IS NOT NULL AND trip_to IS NOT NULL
                   AND distance_km > 0 AND mileage_rate > 0
                   AND amount = round(distance_km * mileage_rate, 2))),
    CONSTRAINT ck_expenses_subsistence_details
        CHECK (claim_type <> 'SUBSISTENCE'
               OR (trip_location IS NOT NULL AND subsistence_days > 0 AND daily_rate > 0
                   AND amount = round(subsistence_days * daily_rate, 2))),

    -- Nobody may review or reimburse their own claim.
    CONSTRAINT ck_expenses_not_self_reviewed
        CHECK (reviewed_by_id IS NULL OR reviewed_by_id <> submitted_by_id),
    CONSTRAINT ck_expenses_not_self_reimbursed
        CHECK (reimbursed_by_id IS NULL OR reimbursed_by_id <> submitted_by_id),

    -- "Who" and "when" are always recorded together.
    CONSTRAINT ck_expenses_review_pair
        CHECK ((reviewed_by_id IS NULL) = (reviewed_at IS NULL)),
    CONSTRAINT ck_expenses_reimbursement_pair
        CHECK ((reimbursed_by_id IS NULL) = (reimbursed_at IS NULL)),

    -- The status must match the recorded decisions.
    CONSTRAINT ck_expenses_review_matches_status
        CHECK ((status = 'SUBMITTED') = (reviewed_by_id IS NULL)),
    CONSTRAINT ck_expenses_reimbursement_matches_status
        CHECK ((status = 'REIMBURSED') = (reimbursed_by_id IS NOT NULL)),
    -- Queries and rejections must tell the worker why.
    CONSTRAINT ck_expenses_review_note_required
        CHECK (status NOT IN ('QUERIED', 'REJECTED') OR review_note IS NOT NULL),
    -- Only money the worker spent themselves is paid back.
    CONSTRAINT ck_expenses_reimburse_own_money_only
        CHECK (status <> 'REIMBURSED' OR paid_with = 'WORKER_OWN_MONEY'),

    CONSTRAINT uq_expenses_company_id_id UNIQUE (company_id, id)
);

-- The manager's review list: "submitted claims in my company, newest first".
CREATE INDEX ix_expenses_company_status
    ON expenses (company_id, status, submitted_at DESC);

-- The worker's list: "my claims, newest first".
CREATE INDEX ix_expenses_submitted_by
    ON expenses (company_id, submitted_by_id, submitted_at DESC);

-- Finding the claims linked to a purchase request.
CREATE INDEX ix_expenses_purchase_request
    ON expenses (company_id, purchase_request_id)
    WHERE purchase_request_id IS NOT NULL;

-- Spotting the same fiscal receipt claimed twice.
CREATE INDEX ix_expenses_fiscal_invoice
    ON expenses (company_id, lower(fiscal_invoice_number))
    WHERE fiscal_invoice_number IS NOT NULL;

CREATE TABLE expense_items (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id               BIGINT         NOT NULL,
    expense_id               BIGINT         NOT NULL,
    line_number              INTEGER        NOT NULL,
    description              VARCHAR(200)   NOT NULL,
    normalized_name          VARCHAR(200),
    quantity                 NUMERIC(12, 3) NOT NULL DEFAULT 1,
    unit                     VARCHAR(10)    NOT NULL DEFAULT 'EACH',
    unit_price               NUMERIC(12, 2),
    line_total               NUMERIC(14, 2) NOT NULL,

    -- Which approved quote item this receipt line matches, and how sure we are.
    purchase_request_item_id BIGINT,
    match_method             VARCHAR(20),
    match_confidence         NUMERIC(4, 3),

    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_expense_items_expense
        FOREIGN KEY (company_id, expense_id) REFERENCES expenses (company_id, id),
    CONSTRAINT fk_expense_items_request_item
        FOREIGN KEY (company_id, purchase_request_item_id)
        REFERENCES purchase_request_items (company_id, id),

    CONSTRAINT ck_expense_items_line_number
        CHECK (line_number > 0),
    CONSTRAINT ck_expense_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_expense_items_unit
        CHECK (unit IN ('EACH', 'LITRE', 'KG', 'METRE', 'PACK', 'BAG', 'BOX')),
    CONSTRAINT ck_expense_items_unit_price
        CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT ck_expense_items_line_total
        CHECK (line_total >= 0),
    CONSTRAINT ck_expense_items_match_method
        CHECK (match_method IS NULL OR match_method IN ('WORKER', 'TEXT_SIMILARITY', 'AI')),
    CONSTRAINT ck_expense_items_match_confidence
        CHECK (match_confidence IS NULL OR match_confidence BETWEEN 0 AND 1),
    -- A match always says how it was made.
    CONSTRAINT ck_expense_items_match_complete
        CHECK ((purchase_request_item_id IS NULL) = (match_method IS NULL)),

    CONSTRAINT uq_expense_items_line UNIQUE (expense_id, line_number),
    CONSTRAINT uq_expense_items_company_id_id UNIQUE (company_id, id)
);