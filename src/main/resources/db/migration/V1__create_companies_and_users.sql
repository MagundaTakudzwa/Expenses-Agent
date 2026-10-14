-- V1: Companies (the businesses using the system) and their users.

CREATE TABLE companies (
    id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                          VARCHAR(150)  NOT NULL,
    base_currency                 VARCHAR(3)    NOT NULL DEFAULT 'USD',
    variance_tolerance_percent    NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    allow_claims_without_approval BOOLEAN       NOT NULL DEFAULT TRUE,
    max_amount_without_approval   NUMERIC(12, 2),
    mileage_rate_per_km           NUMERIC(10, 4),
    daily_subsistence_rate        NUMERIC(12, 2),
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_companies_base_currency
        CHECK (base_currency IN ('USD', 'ZWG', 'ZAR')),
    CONSTRAINT ck_companies_tolerance
        CHECK (variance_tolerance_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_companies_max_without_approval
        CHECK (max_amount_without_approval IS NULL OR max_amount_without_approval >= 0),
    CONSTRAINT ck_companies_mileage_rate
        CHECK (mileage_rate_per_km IS NULL OR mileage_rate_per_km > 0),
    CONSTRAINT ck_companies_subsistence_rate
        CHECK (daily_subsistence_rate IS NULL OR daily_subsistence_rate > 0)
);

CREATE TABLE app_users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id    BIGINT       NOT NULL REFERENCES companies (id),
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_app_users_role
        CHECK (role IN ('WORKER', 'MANAGER', 'FINANCE', 'ADMIN')),
    -- Lets later tables check that a user belongs to the SAME company as the record.
    CONSTRAINT uq_app_users_company_id_id UNIQUE (company_id, id)
);

-- Emails are unique across the whole system, ignoring capital letters.
CREATE UNIQUE INDEX uq_app_users_email ON app_users (lower(email));