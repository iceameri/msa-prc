CREATE TABLE IF NOT EXISTS reports (
    id          BIGSERIAL   PRIMARY KEY,
    reporter_id VARCHAR(50) NOT NULL, -- logical FK: users.id
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    reason      TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reports_target ON reports (target_type, target_id);
