-- V4__API_improvements.sql
-- API 개선을 위한 스키마 변경

-- 1. users 테이블에서 dark_mode 컬럼 삭제 (이미 존재하는 경우)
ALTER TABLE users DROP COLUMN IF EXISTS dark_mode;

-- 2. users 테이블에 department(학과) 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100);

-- 3. detail_category 테이블 생성 (crawl_notice의 category 값 저장용)
CREATE TABLE IF NOT EXISTS detail_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. detail_category 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_detail_category_name ON detail_category(name);
CREATE INDEX IF NOT EXISTS idx_detail_category_is_active ON detail_category(is_active);

-- 5. user_detail_category_subscriptions 테이블 생성
CREATE TABLE IF NOT EXISTS user_detail_category_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    detail_category_id BIGINT NOT NULL REFERENCES detail_category(id) ON DELETE CASCADE,
    subscribed BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_detail_category UNIQUE (user_id, detail_category_id)
);

-- 6. user_detail_category_subscriptions 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_udcs_user_id ON user_detail_category_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_udcs_detail_category_id ON user_detail_category_subscriptions(detail_category_id);
CREATE INDEX IF NOT EXISTS idx_udcs_subscribed ON user_detail_category_subscriptions(subscribed);

-- 7. crawl_notice에서 고유한 category 값을 추출하여 detail_category에 삽입
INSERT INTO detail_category (name, is_active, created_at, updated_at)
SELECT DISTINCT category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM crawl_notice
WHERE category IS NOT NULL AND category != '' AND LENGTH(category) <= 100
ON CONFLICT (name) DO NOTHING;

-- 8. 트리거 함수: detail_category 자동 업데이트 (새 공지사항 추가 시)
CREATE OR REPLACE FUNCTION sync_detail_category()
RETURNS TRIGGER AS $$
BEGIN
    -- 새로운 category 값이 있고, detail_category에 없으면 추가
    IF NEW.category IS NOT NULL AND NEW.category != '' AND LENGTH(NEW.category) <= 100 THEN
        INSERT INTO detail_category (name, is_active, created_at, updated_at)
        VALUES (NEW.category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (name) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 9. crawl_notice에 트리거 추가 (새 공지사항 삽입 시 detail_category 자동 업데이트)
DROP TRIGGER IF EXISTS trigger_sync_detail_category ON crawl_notice;
CREATE TRIGGER trigger_sync_detail_category
    AFTER INSERT ON crawl_notice
    FOR EACH ROW
    EXECUTE FUNCTION sync_detail_category();
