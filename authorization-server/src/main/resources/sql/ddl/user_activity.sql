CREATE TABLE IF NOT EXISTS user_activity (
    user_id        BIGINT      NOT NULL PRIMARY KEY,  -- users.user_id (논리적 FK)
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
