ALTER TABLE authorization_db.public.users
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

ALTER TABLE authorization_db.public.users
    DROP CONSTRAINT IF EXISTS uk_users_username;
ALTER TABLE authorization_db.public.users
    DROP CONSTRAINT IF EXISTS uk_users_email;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_no_tenant
    ON authorization_db.public.users (username) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_no_tenant
    ON authorization_db.public.users (email) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_tenant
    ON authorization_db.public.users (username, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_tenant
    ON authorization_db.public.users (email, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_tenant_id
    ON authorization_db.public.users (tenant_id);
