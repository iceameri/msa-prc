CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGSERIAL    PRIMARY KEY,
    actor_id       VARCHAR(50)  NOT NULL,
    actor_username VARCHAR(50)  NOT NULL,  -- snapshot: 행위 시점 username
    action         VARCHAR(50)  NOT NULL,
    target_type    VARCHAR(50),
    target_id      VARCHAR(100),
    detail         TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id   ON audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor      ON audit_logs (actor_username);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
