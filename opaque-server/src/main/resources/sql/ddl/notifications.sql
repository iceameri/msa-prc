CREATE TABLE IF NOT EXISTS notifications (
    id                 BIGSERIAL   PRIMARY KEY,
    recipient_id       VARCHAR(50) NOT NULL,
    recipient_username VARCHAR(50) NOT NULL,  -- snapshot: 발송 시점 username
    type               VARCHAR(50) NOT NULL,
    content            TEXT        NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_id ON notifications (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status       ON notifications (status);
