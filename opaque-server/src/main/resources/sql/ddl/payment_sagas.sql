CREATE TABLE IF NOT EXISTS payment_sagas (
    id         BIGSERIAL   PRIMARY KEY,
    payment_id BIGINT      NOT NULL, -- logical FK: payments.id
    step       VARCHAR(50) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    detail     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_sagas_payment_id ON payment_sagas (payment_id);
