CREATE TABLE IF NOT EXISTS payments (
    id         BIGSERIAL      PRIMARY KEY,
    user_id    VARCHAR(50)    NOT NULL, -- logical FK: users.id
    order_id   VARCHAR(100)   NOT NULL UNIQUE,
    amount     NUMERIC(15, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_user_id  ON payments (user_id);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status   ON payments (status);
