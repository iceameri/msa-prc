-- 1. authorization_system_clients 테이블 생성
CREATE TABLE IF NOT EXISTS jwt_db.public.authorization_system_clients (
    client_id    VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_authorization_system_clients PRIMARY KEY (client_id)
);

INSERT INTO jwt_db.public.authorization_system_clients (client_id, display_name)
VALUES ('jwt-server', 'JWT Server'), ('opaque-server', 'Opaque Server')
ON CONFLICT (client_id) DO NOTHING;

-- 2. posts: author_id nullable, client_id 추가, XOR CHECK constraint
ALTER TABLE jwt_db.public.posts ALTER COLUMN author_id DROP NOT NULL;
ALTER TABLE jwt_db.public.posts ADD COLUMN IF NOT EXISTS client_id VARCHAR(50) NULL;
ALTER TABLE jwt_db.public.posts ADD CONSTRAINT chk_posts_author CHECK (
    (author_id IS NOT NULL AND client_id IS NULL) OR
    (author_id IS NULL     AND client_id IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_posts_client_id ON jwt_db.public.posts (client_id);

-- 3. comments: author_id nullable, client_id 추가, XOR CHECK constraint
ALTER TABLE jwt_db.public.comments ALTER COLUMN author_id DROP NOT NULL;
ALTER TABLE jwt_db.public.comments ADD COLUMN IF NOT EXISTS client_id VARCHAR(50) NULL;
ALTER TABLE jwt_db.public.comments ADD CONSTRAINT chk_comments_author CHECK (
    (author_id IS NOT NULL AND client_id IS NULL) OR
    (author_id IS NULL     AND client_id IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_comments_client_id ON jwt_db.public.comments (client_id);
