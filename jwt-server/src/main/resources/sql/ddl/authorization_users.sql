CREATE TABLE IF NOT EXISTS authorization_users (
    id         BIGINT      NOT NULL PRIMARY KEY,  -- authorization_db users.id
    username   VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- 이벤트 발행 시각을 저장해 순서 역전된 메시지를 거부하는 기준으로 사용
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_authorization_users_username ON authorization_users (username);
