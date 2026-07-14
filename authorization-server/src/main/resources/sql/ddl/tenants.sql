CREATE TABLE IF NOT EXISTS tenants
(
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(50)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants (slug);
