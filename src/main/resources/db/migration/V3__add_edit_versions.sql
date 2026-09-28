-- ADR-18: edit version for detecting a save made from an out-of-date form (lost update).
-- The version goes up by one only when an edit form changes the content (plan fields; todo title, due date,
-- priority, estimate, tags; review improvement). Completion, reopen, and improvement transfer leave it as is
-- (ADR-18 E7), and a save that changes nothing leaves it as is (E5). It is not an authorization value.
-- Existing rows start at 0. Adding a NOT NULL column with a constant default does not rewrite the table.

ALTER TABLE plans ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE plans ADD CONSTRAINT ck_plans_version CHECK (version >= 0);

ALTER TABLE todos ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE todos ADD CONSTRAINT ck_todos_version CHECK (version >= 0);

ALTER TABLE reviews ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE reviews ADD CONSTRAINT ck_reviews_version CHECK (version >= 0);
