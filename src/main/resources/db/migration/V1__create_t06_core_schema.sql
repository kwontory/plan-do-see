-- PlanDoSee T06 core schema (contract pds-schema-v2 2.0.0)
-- T07 auth tables (auth_identities, password_credentials) are added by a later migration.

CREATE TABLE users (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    email             VARCHAR(320) NULL,
    normalized_email  VARCHAR(320) NULL,
    nickname          VARCHAR(100) NOT NULL,
    email_verified_at TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMPTZ  NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ux_users_normalized_email_active
    ON users (normalized_email)
    WHERE deleted_at IS NULL AND normalized_email IS NOT NULL;

CREATE TABLE plans (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    start_date          DATE          NOT NULL,
    end_date            DATE          NOT NULL,
    priority            VARCHAR(20)   NOT NULL,
    success_criteria    VARCHAR(1000) NOT NULL,
    estimated_minutes   INTEGER       NOT NULL,
    carried_improvement TEXT          NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMPTZ   NULL,
    CONSTRAINT pk_plans PRIMARY KEY (id),
    CONSTRAINT fk_plans_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_plans_title CHECK (char_length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT ck_plans_period CHECK (end_date >= start_date),
    CONSTRAINT ck_plans_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_plans_success_criteria CHECK (char_length(btrim(success_criteria)) BETWEEN 1 AND 1000),
    CONSTRAINT ck_plans_estimated_minutes CHECK (estimated_minutes BETWEEN 0 AND 525600)
);

CREATE INDEX ix_plans_user_created_active
    ON plans (user_id, created_at, id)
    WHERE deleted_at IS NULL;

CREATE TABLE plan_revisions (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    plan_id             UUID          NOT NULL,
    revision_no         INTEGER       NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    start_date          DATE          NOT NULL,
    end_date            DATE          NOT NULL,
    priority            VARCHAR(20)   NOT NULL,
    success_criteria    VARCHAR(1000) NOT NULL,
    estimated_minutes   INTEGER       NOT NULL,
    carried_improvement TEXT          NULL,
    revised_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_plan_revisions PRIMARY KEY (id),
    CONSTRAINT fk_plan_revisions_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT uq_plan_revisions_plan_no UNIQUE (plan_id, revision_no),
    CONSTRAINT ck_plan_revisions_no CHECK (revision_no >= 1),
    CONSTRAINT ck_plan_revisions_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_plan_revisions_estimated_minutes CHECK (estimated_minutes >= 0)
);

CREATE TABLE todos (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    plan_id           UUID         NOT NULL,
    title             VARCHAR(200) NOT NULL,
    due_date          DATE         NULL,
    priority          VARCHAR(20)  NOT NULL,
    estimated_minutes INTEGER      NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    completion_cycle  INTEGER      NOT NULL DEFAULT 0,
    completed_at      TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMPTZ  NULL,
    CONSTRAINT pk_todos PRIMARY KEY (id),
    CONSTRAINT fk_todos_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT ck_todos_title CHECK (char_length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT ck_todos_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_todos_estimated_minutes CHECK (estimated_minutes BETWEEN 0 AND 525600),
    CONSTRAINT ck_todos_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_todos_completion_cycle CHECK (completion_cycle >= 0),
    CONSTRAINT ck_todos_completed_at CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status = 'IN_PROGRESS' AND completed_at IS NULL)
    )
);

CREATE INDEX ix_todos_plan_status_due_active
    ON todos (plan_id, status, due_date, id)
    WHERE deleted_at IS NULL;

CREATE TABLE tags (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    name            VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMPTZ NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_tags_name CHECK (char_length(btrim(name)) BETWEEN 1 AND 50),
    CONSTRAINT ck_tags_normalized_name CHECK (normalized_name = lower(btrim(name)))
);

CREATE UNIQUE INDEX ux_tags_user_normalized_name_active
    ON tags (user_id, normalized_name)
    WHERE deleted_at IS NULL;

CREATE TABLE todo_tags (
    todo_id    UUID        NOT NULL,
    tag_id     UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_todo_tags PRIMARY KEY (todo_id, tag_id),
    CONSTRAINT fk_todo_tags_todo FOREIGN KEY (todo_id) REFERENCES todos (id),
    CONSTRAINT fk_todo_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE INDEX ix_todo_tags_tag ON todo_tags (tag_id, todo_id);

CREATE TABLE execution_logs (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    todo_id        UUID        NOT NULL,
    started_at     TIMESTAMPTZ NOT NULL,
    ended_at       TIMESTAMPTZ NOT NULL,
    actual_minutes INTEGER     NOT NULL,
    blocker_reason TEXT        NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_execution_logs PRIMARY KEY (id),
    CONSTRAINT fk_execution_logs_todo FOREIGN KEY (todo_id) REFERENCES todos (id),
    CONSTRAINT ck_execution_logs_range CHECK (ended_at >= started_at),
    CONSTRAINT ck_execution_logs_actual_minutes CHECK (actual_minutes >= 0),
    CONSTRAINT ck_execution_logs_blocker_reason CHECK (blocker_reason IS NULL OR char_length(btrim(blocker_reason)) >= 1)
);

CREATE INDEX ix_execution_logs_todo_started ON execution_logs (todo_id, started_at, id);

CREATE TABLE completion_events (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    todo_id         UUID        NOT NULL,
    idempotency_key UUID        NOT NULL,
    cycle_no        INTEGER     NOT NULL,
    completed_at    TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_completion_events PRIMARY KEY (id),
    CONSTRAINT fk_completion_events_todo FOREIGN KEY (todo_id) REFERENCES todos (id),
    CONSTRAINT uq_completion_events_todo_key UNIQUE (todo_id, idempotency_key),
    CONSTRAINT uq_completion_events_todo_cycle UNIQUE (todo_id, cycle_no),
    CONSTRAINT ck_completion_events_cycle CHECK (cycle_no >= 1)
);

CREATE TABLE reviews (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    plan_id        UUID        NOT NULL,
    period_start   DATE        NOT NULL,
    period_end     DATE        NOT NULL,
    improvement    TEXT        NULL,
    next_plan_id   UUID        NULL,
    transferred_at TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT fk_reviews_next_plan FOREIGN KEY (next_plan_id) REFERENCES plans (id),
    CONSTRAINT ck_reviews_period CHECK (period_end >= period_start),
    CONSTRAINT ck_reviews_transfer CHECK (
        (next_plan_id IS NULL AND transferred_at IS NULL)
        OR (next_plan_id IS NOT NULL AND transferred_at IS NOT NULL)
    )
);

CREATE INDEX ix_reviews_user_plan_period_active
    ON reviews (user_id, plan_id, period_start, period_end)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_reviews_next_plan ON reviews (next_plan_id) WHERE next_plan_id IS NOT NULL;

-- T06 demo owner. No real email (CLAUDE.md 5장).
INSERT INTO users (id, email, normalized_email, nickname, email_verified_at)
VALUES ('00000000-0000-4000-8000-000000000001', NULL, NULL, 'demo', NULL);
