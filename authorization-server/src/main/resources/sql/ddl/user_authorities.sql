CREATE TABLE IF NOT EXISTS user_authorities (
    user_id   BIGINT           NOT NULL, -- logical FK: users.id
    authority VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, authority)
);

CREATE INDEX IF NOT EXISTS idx_user_authorities_user_id ON user_authorities (user_id);
