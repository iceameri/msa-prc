CREATE TABLE IF NOT EXISTS reports (
    id                BIGSERIAL   PRIMARY KEY,
    external_id       BIGINT      NOT NULL UNIQUE, -- jwt-server report ID
    reporter_username VARCHAR(50) NOT NULL,
    target_type       VARCHAR(20) NOT NULL,
    target_id         BIGINT      NOT NULL,
    reason            TEXT        NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by       VARCHAR(50),
    reviewed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reports_status      ON reports (status);
CREATE INDEX IF NOT EXISTS idx_reports_external_id ON reports (external_id);
