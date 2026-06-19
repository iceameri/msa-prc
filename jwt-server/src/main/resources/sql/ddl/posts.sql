CREATE TABLE IF NOT EXISTS posts (
    id            BIGSERIAL    PRIMARY KEY,
    author_id     BIGINT       NULL,       -- logical FK: authorization_users.id
    client_id     VARCHAR(50)  NULL,       -- logical FK: system_clients.client_id
    title         VARCHAR(255) NOT NULL,
    content       TEXT         NOT NULL,
    image_url     VARCHAR(500),
    like_count    INTEGER      NOT NULL DEFAULT 0,
    comment_count INTEGER      NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_posts_author CHECK (
        (author_id IS NOT NULL AND client_id IS NULL) OR
        (author_id IS NULL     AND client_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_posts_author_id  ON posts (author_id);
CREATE INDEX IF NOT EXISTS idx_posts_client_id  ON posts (client_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_status     ON posts (status);
