CREATE TABLE IF NOT EXISTS users
(
    user_id        BIGSERIAL    NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    password       VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL,
    full_name      VARCHAR(100),
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / SUSPENDED / BANNED / DELETED
    login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until   TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    mfa_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret     VARCHAR(64),
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
