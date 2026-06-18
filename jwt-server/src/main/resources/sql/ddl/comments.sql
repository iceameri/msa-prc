CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL   PRIMARY KEY,
    post_id    BIGINT      NOT NULL, -- logical FK: posts.id
    author_id  BIGINT      NOT NULL, -- logical FK: authorization_users.id
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_post_id   ON comments (post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);
