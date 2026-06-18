CREATE TABLE IF NOT EXISTS hashtags (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS post_hashtags (
    post_id    BIGINT NOT NULL, -- logical FK: posts.id
    hashtag_id BIGINT NOT NULL, -- logical FK: hashtags.id
    PRIMARY KEY (post_id, hashtag_id)
);

CREATE INDEX IF NOT EXISTS idx_post_hashtags_hashtag_id ON post_hashtags (hashtag_id);
