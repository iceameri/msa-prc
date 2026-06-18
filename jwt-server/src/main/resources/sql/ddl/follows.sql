CREATE TABLE IF NOT EXISTS follows (
    follower_id  VARCHAR(50) NOT NULL, -- logical FK: users.id
    following_id VARCHAR(50) NOT NULL, -- logical FK: users.id
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_following_id ON follows (following_id);
