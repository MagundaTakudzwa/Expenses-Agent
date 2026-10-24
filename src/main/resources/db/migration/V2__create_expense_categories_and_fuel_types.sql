-- V2: Expense categories (each company manages its own) and fuel types for the fuel picker.

CREATE TABLE expense_categories (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies (id),
    name             VARCHAR(100) NOT NULL,
    claim_type       VARCHAR(20)  NOT NULL,
    requires_receipt BOOLEAN      NOT NULL DEFAULT TRUE,
    payroll_code     VARCHAR(30),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_expense_categories_claim_type
        CHECK (claim_type IN ('RECEIPT', 'FUEL', 'MILEAGE', 'SUBSISTENCE')),
    CONSTRAINT uq_expense_categories_company_id_id UNIQUE (company_id, id)
);

-- Category names are unique within a company, ignoring capital letters.
CREATE UNIQUE INDEX uq_expense_categories_company_name
    ON expense_categories (company_id, lower(name));

CREATE TABLE fuel_types (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT      NOT NULL REFERENCES companies (id),
    name       VARCHAR(50) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_fuel_types_company_id_id UNIQUE (company_id, id)
);

CREATE UNIQUE INDEX uq_fuel_types_company_name
    ON fuel_types (company_id, lower(name));