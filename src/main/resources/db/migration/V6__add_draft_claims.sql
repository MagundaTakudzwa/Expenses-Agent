-- V6: Claims start as drafts. A worker can attach several receipts to one approved
-- purchase request, then submit them all together when they've finished buying.

-- 1. Add DRAFT to the allowed statuses, and make it the starting status.
ALTER TABLE expenses DROP CONSTRAINT ck_expenses_status;
ALTER TABLE expenses ADD CONSTRAINT ck_expenses_status
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'VERIFIED', 'QUERIED', 'REJECTED', 'REIMBURSED'));
ALTER TABLE expenses ALTER COLUMN status SET DEFAULT 'DRAFT';

-- 2. A draft hasn't been submitted yet, so it has no submission time.
ALTER TABLE expenses ALTER COLUMN submitted_at DROP DEFAULT;
ALTER TABLE expenses ALTER COLUMN submitted_at DROP NOT NULL;
ALTER TABLE expenses ADD CONSTRAINT ck_expenses_draft_not_submitted
    CHECK ((status = 'DRAFT') = (submitted_at IS NULL));

-- 3. Drafts, like newly submitted claims, have no reviewer yet.
ALTER TABLE expenses DROP CONSTRAINT ck_expenses_review_matches_status;
ALTER TABLE expenses ADD CONSTRAINT ck_expenses_review_matches_status
    CHECK ((status IN ('DRAFT', 'SUBMITTED')) = (reviewed_by_id IS NULL));

-- 4. Speeds up "this request's receipts" and "my drafts".
CREATE INDEX ix_expenses_drafts
    ON expenses (company_id, submitted_by_id)
    WHERE status = 'DRAFT';