CREATE TABLE IF NOT EXISTS outbox_events (
    id             BIGSERIAL    PRIMARY KEY,
    aggregate_id   VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    claimed_at     TIMESTAMPTZ,
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_unclaimed ON outbox_events (created_at ASC, id ASC) WHERE claimed_at IS NULL AND sent_at IS NULL;
