-- ADR-30 (QA input validation audit IV-01, IV-02, IV-03, IV-17): the DB holds the same limits as the service rules.
--   * Dates: DateBounds 1900-01-01 .. 2999-12-31, both ends included (ADR-29). Plan period and todo due date as
--     DATE; execution start and end by their Asia/Seoul local date, exactly like the service (InputCheck.dateTime):
--     AT TIME ZONE uses the same zone rules, including Seoul local mean time (+08:27:52) before 1908.
--     -infinity / infinity are outside the range, so they are rejected too.
--   * One execution record: at most 525600 minutes (365 days, ExecutionRules.PERIOD_MAX_MINUTES).
--   * Improvement text: at most 1000 characters after trim, LF line breaks (ReviewRules.IMPROVEMENT_MAX), on the
--     review and on the copy carried into the next plan. V1 had no length check here.
-- The application checks every value first; these are the last line of defence and map to the same error codes
-- (PlanConstraintCodes, TodoConstraintCodes, ExecutionConstraintCodes, ReviewConstraintCodes).
--
-- NOT VALID (like V4): new and updated rows are checked from now on, rows stored before are not scanned. A shared
-- database may already hold values outside these limits (the audit stored year 0, BC and infinity values through
-- hand-made requests); validating would make this migration, and so the application start, fail. Execution logs are
-- audit records the app never changes. Note that PostgreSQL checks the whole new row version on UPDATE, so an old
-- plan or todo with an out-of-range date must get a valid date when it is edited; completing or reopening such a todo
-- shows the date-range notice instead of saving. Deleting a todo is always possible: deleted rows are exempt.
-- Find old rows (read only):
--   SELECT id FROM plans WHERE start_date NOT BETWEEN DATE '1900-01-01' AND DATE '2999-12-31'
--                           OR end_date NOT BETWEEN DATE '1900-01-01' AND DATE '2999-12-31' ORDER BY id;
--   SELECT id FROM todos WHERE deleted_at IS NULL AND due_date NOT BETWEEN DATE '1900-01-01' AND DATE '2999-12-31' ORDER BY id;
--   SELECT id FROM execution_logs
--    WHERE (started_at AT TIME ZONE 'Asia/Seoul')::date NOT BETWEEN DATE '1900-01-01' AND DATE '2999-12-31'
--       OR (ended_at AT TIME ZONE 'Asia/Seoul')::date NOT BETWEEN DATE '1900-01-01' AND DATE '2999-12-31'
--       OR actual_minutes > 525600 ORDER BY id;
--   SELECT id FROM reviews WHERE char_length(btrim(improvement)) > 1000 ORDER BY id;
--   SELECT id FROM plans WHERE char_length(btrim(carried_improvement)) > 1000 ORDER BY id;
-- Once none are left, a later migration may run ALTER TABLE ... VALIDATE CONSTRAINT <name> for each.
-- Adding a NOT VALID CHECK only takes a brief lock and does not rewrite or scan the table.

ALTER TABLE plans
    ADD CONSTRAINT ck_plans_start_date_range
    CHECK (start_date BETWEEN DATE '1900-01-01' AND DATE '2999-12-31') NOT VALID;

ALTER TABLE plans
    ADD CONSTRAINT ck_plans_end_date_range
    CHECK (end_date BETWEEN DATE '1900-01-01' AND DATE '2999-12-31') NOT VALID;

ALTER TABLE plans
    ADD CONSTRAINT ck_plans_carried_improvement_max
    CHECK (carried_improvement IS NULL OR char_length(btrim(carried_improvement)) <= 1000) NOT VALID;

ALTER TABLE todos
    ADD CONSTRAINT ck_todos_due_date_range
    CHECK (deleted_at IS NOT NULL OR due_date IS NULL
           OR due_date BETWEEN DATE '1900-01-01' AND DATE '2999-12-31') NOT VALID;

ALTER TABLE execution_logs
    ADD CONSTRAINT ck_execution_logs_started_at_range
    CHECK ((started_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN DATE '1900-01-01' AND DATE '2999-12-31') NOT VALID;

ALTER TABLE execution_logs
    ADD CONSTRAINT ck_execution_logs_ended_at_range
    CHECK ((ended_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN DATE '1900-01-01' AND DATE '2999-12-31') NOT VALID;

ALTER TABLE execution_logs
    ADD CONSTRAINT ck_execution_logs_actual_minutes_max
    CHECK (actual_minutes <= 525600) NOT VALID;

ALTER TABLE reviews
    ADD CONSTRAINT ck_reviews_improvement_max
    CHECK (improvement IS NULL OR char_length(btrim(improvement)) <= 1000) NOT VALID;
