-- V1__init_user_schema.sql
-- Initial schema: users + user_preferences

CREATE TABLE IF NOT EXISTS users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(255)    NOT NULL UNIQUE,   -- Auth0 sub claim (e.g., "auth0|abc123")
    email           VARCHAR(255)    NOT NULL UNIQUE,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    phone_number    VARCHAR(30),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUSPENDED | DELETED
    deleted_at      TIMESTAMPTZ,                                -- GDPR soft delete
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language            VARCHAR(10) NOT NULL DEFAULT 'en',
    timezone            VARCHAR(50) NOT NULL DEFAULT 'UTC',
    email_notifications BOOLEAN     NOT NULL DEFAULT TRUE,
    push_notifications  BOOLEAN     NOT NULL DEFAULT TRUE,
    theme               VARCHAR(20) NOT NULL DEFAULT 'LIGHT',   -- LIGHT | DARK | SYSTEM
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_preferences_user_id UNIQUE (user_id)
);

-- Audit log: captures every sensitive action for GDPR accountability
CREATE TABLE IF NOT EXISTS user_audit_log (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     UUID            NOT NULL,
    action      VARCHAR(100)    NOT NULL,   -- e.g., REGISTERED, PROFILE_UPDATED, ACCOUNT_DELETED
    performed_by VARCHAR(255),              -- sub of the actor (could be the user or an admin)
    ip_address  VARCHAR(45),
    details     TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_users_external_id ON users(external_id);
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_status      ON users(status);
CREATE INDEX idx_audit_user_id     ON user_audit_log(user_id);