CREATE TABLE IF NOT EXISTS likes (
    post_id    BIGINT      NOT NULL, -- logical FK: posts.id
    user_id    VARCHAR(50) NOT NULL, -- logical FK: users.id
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);
