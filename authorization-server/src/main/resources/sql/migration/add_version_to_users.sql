ALTER TABLE authorization_db.public.users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
