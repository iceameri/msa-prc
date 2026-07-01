CREATE TABLE IF NOT EXISTS authorization_users
(
    user_id    BIGINT      NOT NULL,
    username   VARCHAR(50) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_authorization_users PRIMARY KEY (user_id),
    CONSTRAINT uk_authorization_users_username UNIQUE (username)
);
