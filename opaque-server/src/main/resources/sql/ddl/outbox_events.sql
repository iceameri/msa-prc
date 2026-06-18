CREATE TABLE IF NOT EXISTS outbox_events (
    id            BIGSERIAL    PRIMARY KEY,
    topic         VARCHAR(100) NOT NULL,
    aggregate_key VARCHAR(255) NOT NULL,
    payload       TEXT         NOT NULL,
    claimed_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 미처리 이벤트만 인덱싱 (릴레이 조회 최적화)
CREATE INDEX IF NOT EXISTS idx_outbox_unclaimed ON outbox_events (created_at ASC, id ASC) WHERE claimed_at IS NULL;
