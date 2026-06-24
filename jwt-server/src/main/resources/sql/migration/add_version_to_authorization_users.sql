ALTER TABLE jwt_db.public.authorization_users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
