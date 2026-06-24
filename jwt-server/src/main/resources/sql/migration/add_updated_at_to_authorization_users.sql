-- 순서 역전 방지를 위한 updated_at 컬럼 추가
-- 이벤트 payload의 updatedAt과 비교해 오래된 메시지를 무시하는 데 사용
ALTER TABLE jwt_db.public.authorization_users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
