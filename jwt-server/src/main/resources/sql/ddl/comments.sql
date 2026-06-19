CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL   PRIMARY KEY,
    post_id    BIGINT      NOT NULL, -- logical FK: posts.id
    author_id  BIGINT      NULL,     -- logical FK: authorization_users.id
    client_id  VARCHAR(50) NULL,     -- logical FK: system_clients.client_id
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_comments_author CHECK (
        (author_id IS NOT NULL AND client_id IS NULL) OR
        (author_id IS NULL     AND client_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_comments_post_id   ON comments (post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);
CREATE INDEX IF NOT EXISTS idx_comments_client_id ON comments (client_id);
