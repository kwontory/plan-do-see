-- ADR-16: todo edit history and reopen events (contract pds-schema-v2 2.1.0).
-- Both tables are immutable audit records: rows are only inserted, never updated or deleted by the application.
-- Ownership is checked through the parent todo and its plan (todos.plan_id -> plans.user_id).

-- Values of a todo just before a successful edit (same idea as plan_revisions, T06-C08). tag_names is a snapshot
-- of the display names at that moment, ordered by normalized name, so later tag changes do not rewrite history.
CREATE TABLE todo_revisions (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    todo_id           UUID         NOT NULL,
    revision_no       INTEGER      NOT NULL,
    title             VARCHAR(200) NOT NULL,
    due_date          DATE         NULL,
    priority          VARCHAR(20)  NOT NULL,
    estimated_minutes INTEGER      NOT NULL,
    tag_names         TEXT[]       NOT NULL DEFAULT '{}',
    revised_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_todo_revisions PRIMARY KEY (id),
    CONSTRAINT fk_todo_revisions_todo FOREIGN KEY (todo_id) REFERENCES todos (id),
    CONSTRAINT uq_todo_revisions_todo_no UNIQUE (todo_id, revision_no),
    CONSTRAINT ck_todo_revisions_no CHECK (revision_no >= 1),
    CONSTRAINT ck_todo_revisions_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_todo_revisions_estimated_minutes CHECK (estimated_minutes >= 0)
);

-- A completed todo put back to IN_PROGRESS. cycle_no is the completion cycle that was undone; the next completion
-- uses cycle_no + 1, so each cycle can be reopened at most once. The reopened cycle must be a recorded completion.
CREATE TABLE reopen_events (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    todo_id     UUID        NOT NULL,
    cycle_no    INTEGER     NOT NULL,
    reopened_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reopen_events PRIMARY KEY (id),
    CONSTRAINT fk_reopen_events_todo FOREIGN KEY (todo_id) REFERENCES todos (id),
    CONSTRAINT fk_reopen_events_completion FOREIGN KEY (todo_id, cycle_no)
        REFERENCES completion_events (todo_id, cycle_no),
    CONSTRAINT uq_reopen_events_todo_cycle UNIQUE (todo_id, cycle_no),
    CONSTRAINT ck_reopen_events_cycle CHECK (cycle_no >= 1)
);
