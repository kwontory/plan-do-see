-- ADR-40: execution records can be edited (start, end, blocker reason), keeping the previous values.
--   * execution_logs.version: edit-conflict detection like plans, todos and reviews (ADR-18); +1 per saved edit.
--     Existing rows start at 0.
--   * execution_log_revisions: the values just before each edit (revision_no 1, 2, ... per record), written in the same
--     transaction as the update. Past values are copies, so no range checks (like plan_revisions, todo_revisions).
-- Overlapping records are refused by the application under a per-person lock (a CHECK cannot compare rows). Records
-- that already overlap stay as they are; docs/workflow/operations.md has the read-only SQL that finds them.

ALTER TABLE execution_logs ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE execution_logs ADD CONSTRAINT ck_execution_logs_version CHECK (version >= 0);

CREATE TABLE execution_log_revisions (
    id               UUID        NOT NULL,
    execution_log_id UUID        NOT NULL,
    revision_no      INTEGER     NOT NULL,
    started_at       TIMESTAMPTZ NOT NULL,
    ended_at         TIMESTAMPTZ NOT NULL,
    actual_minutes   INTEGER     NOT NULL,
    blocker_reason   TEXT        NULL,
    revised_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_execution_log_revisions PRIMARY KEY (id),
    CONSTRAINT fk_execution_log_revisions_log FOREIGN KEY (execution_log_id) REFERENCES execution_logs (id),
    CONSTRAINT uq_execution_log_revisions_log_no UNIQUE (execution_log_id, revision_no),
    CONSTRAINT ck_execution_log_revisions_no CHECK (revision_no >= 1)
);
