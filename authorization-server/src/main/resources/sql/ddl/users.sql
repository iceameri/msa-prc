CREATE TABLE IF NOT EXISTS users
(
    user_id        BIGSERIAL    NOT NULL,
    tenant_id      BIGINT,
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
    version        BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- Platform users (tenant_id IS NULL): unique username and email
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_no_tenant ON users (username) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_no_tenant ON users (email) WHERE tenant_id IS NULL;
-- Tenant users: unique (username, tenant_id) and (email, tenant_id)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_tenant ON users (username, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_tenant ON users (email, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users (tenant_id);
