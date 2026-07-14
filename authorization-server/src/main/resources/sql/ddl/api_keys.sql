CREATE TABLE IF NOT EXISTS api_keys
(
    id                BIGSERIAL    NOT NULL,
    tenant_id         BIGINT       NOT NULL,
    key_hash          VARCHAR(64)  NOT NULL,
    key_prefix        VARCHAR(12)  NOT NULL,
    name              VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    rate_limit_burst  INT          NOT NULL DEFAULT 100,
    rate_limit_refill INT          NOT NULL DEFAULT 50,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ,
    last_used_at      TIMESTAMPTZ,
    CONSTRAINT pk_api_keys PRIMARY KEY (id),
    CONSTRAINT uk_api_keys_key_hash UNIQUE (key_hash)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant_id ON api_keys (tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_key_prefix ON api_keys (key_prefix);
