-- ADR-22 amendment 2 (QA security review M3): upper limit for execution_logs.blocker_reason, the same 1000 characters as the
-- other multi-line inputs (plans.success_criteria, reviews.improvement). ExecutionRules.BLOCKER_REASON_MAX, the form's
-- @Size, and ExecutionService's entry check use the same value; the length counts LF line breaks as one character.
--
-- NOT VALID: the constraint is enforced for every new or updated row from now on, but rows stored before this
-- migration are not scanned. V1 had no upper limit, so a shared database may already hold a longer reason; validating
-- would make this migration (and so the application start) fail, and execution logs are audit records that the app
-- never edits or deletes (CLAUDE.md 5장). Existing over-long rows stay readable and exportable.
-- Find them with: SELECT id FROM execution_logs WHERE char_length(btrim(blocker_reason)) > 1000 ORDER BY id;
-- Once none are left, a later migration may run
--   ALTER TABLE execution_logs VALIDATE CONSTRAINT ck_execution_logs_blocker_reason_max;
-- Adding a NOT VALID CHECK only takes a brief lock and does not rewrite or scan the table.

ALTER TABLE execution_logs
    ADD CONSTRAINT ck_execution_logs_blocker_reason_max
    CHECK (blocker_reason IS NULL OR char_length(btrim(blocker_reason)) <= 1000) NOT VALID;
