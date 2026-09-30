-- T07 sign-up, log-in, sessions (ADR-33 amendments 1-2, ADR-34, ADR-37).
--   * auth_identities: one way to log in = one row. Only LOCAL (login id + password) exists for now; the login id is
--     stored normalized (lower case) as provider_user_id and is unique per provider (duplicate sign-ups, also
--     concurrent ones, fail on uq_auth_identities_provider_user). Login id rule = AuthRules (4-20, a-z 0-9 _).
--   * password_credentials: the bcrypt hash only (never the password). Hash shape = AuthRules / Spring Security
--     BCryptPasswordEncoder output ($2a$, $2b$ or $2y$, two-digit cost, 53 characters of salt and hash).
--   * login_attempts: login failures and sign-up attempts for brute-force blocking (ADR-37). Neither the login id
--     text nor any typed secret is kept; the login key is the SHA-256 hex of the normalized login id. Not owned data (no user_id), no soft
--     delete: rows older than a day are deleted by the application.
--   * SPRING_SESSION / SPRING_SESSION_ATTRIBUTES: server sessions of Spring Session JDBC 4.1.1, exactly its
--     schema-postgresql.sql (spring.session.jdbc.initialize-schema=never). The library runs the SQL on these tables.
--   * users.nickname: 1-20 characters after trim (UserRules). NOT VALID like V4/V5: stored rows are not scanned.
-- The demo user row (V1) stays; it has no login identity and is only the owner of T06 data until it is transferred.

CREATE TABLE auth_identities (
    id                      UUID         NOT NULL,
    user_id                 UUID         NOT NULL,
    provider                VARCHAR(30)  NOT NULL,
    provider_user_id        VARCHAR(255) NOT NULL,
    provider_email          VARCHAR(320) NULL,
    provider_email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    linked_at               TIMESTAMPTZ  NOT NULL,
    last_login_at           TIMESTAMPTZ  NULL,
    CONSTRAINT pk_auth_identities PRIMARY KEY (id),
    CONSTRAINT fk_auth_identities_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_auth_identities_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_auth_identities_provider CHECK (provider IN ('LOCAL')),
    CONSTRAINT ck_auth_identities_local_login_id
        CHECK (provider <> 'LOCAL' OR provider_user_id ~ '^[a-z0-9_]{4,20}$'),
    CONSTRAINT ck_auth_identities_local_no_email
        CHECK (provider <> 'LOCAL' OR (provider_email IS NULL AND provider_email_verified = FALSE))
);

-- At most one LOCAL login per person.
CREATE UNIQUE INDEX ux_auth_identities_user_local ON auth_identities (user_id) WHERE provider = 'LOCAL';

CREATE TABLE password_credentials (
    user_id             UUID         NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_password_credentials PRIMARY KEY (user_id),
    CONSTRAINT fk_password_credentials_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_password_credentials_bcrypt
        CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$')
);

CREATE TABLE login_attempts (
    id             UUID         NOT NULL,
    kind           VARCHAR(10)  NOT NULL,
    login_key_hash VARCHAR(64)  NULL,
    client_ip      VARCHAR(100) NOT NULL,
    result         VARCHAR(10)  NOT NULL,
    attempted_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_login_attempts PRIMARY KEY (id),
    CONSTRAINT ck_login_attempts_kind CHECK (kind IN ('LOGIN', 'SIGNUP')),
    CONSTRAINT ck_login_attempts_result CHECK (result IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_login_attempts_key CHECK (
        (kind = 'LOGIN' AND login_key_hash IS NOT NULL AND login_key_hash ~ '^[0-9a-f]{64}$')
        OR (kind = 'SIGNUP' AND login_key_hash IS NULL)
    ),
    CONSTRAINT ck_login_attempts_client_ip CHECK (char_length(client_ip) BETWEEN 1 AND 100)
);

-- Login id + IP failures, IP failures, sign-ups per IP (each within a time window), and the daily clean-up.
CREATE INDEX ix_login_attempts_key_ip ON login_attempts (kind, login_key_hash, client_ip, attempted_at);
CREATE INDEX ix_login_attempts_ip ON login_attempts (kind, client_ip, attempted_at);
CREATE INDEX ix_login_attempts_attempted_at ON login_attempts (attempted_at, id);

ALTER TABLE users
    ADD CONSTRAINT ck_users_nickname CHECK (char_length(btrim(nickname)) BETWEEN 1 AND 20) NOT VALID;

-- Spring Session JDBC 4.1.1 schema-postgresql.sql (unchanged).
CREATE TABLE SPRING_SESSION (
	PRIMARY_ID CHAR(36) NOT NULL,
	SESSION_ID CHAR(36) NOT NULL,
	CREATION_TIME BIGINT NOT NULL,
	LAST_ACCESS_TIME BIGINT NOT NULL,
	MAX_INACTIVE_INTERVAL INT NOT NULL,
	EXPIRY_TIME BIGINT NOT NULL,
	PRINCIPAL_NAME VARCHAR(100),
	CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
	SESSION_PRIMARY_ID CHAR(36) NOT NULL,
	ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
	ATTRIBUTE_BYTES BYTEA NOT NULL,
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
