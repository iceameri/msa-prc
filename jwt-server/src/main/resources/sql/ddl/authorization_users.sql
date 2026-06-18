CREATE TABLE IF NOT EXISTS authorization_users (
    id         BIGINT      NOT NULL PRIMARY KEY,  -- authorization_db users.id
    username   VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_authorization_users_username ON authorization_users (username);
