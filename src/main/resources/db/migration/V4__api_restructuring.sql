-- V4: API 구조 개편
-- 1. departments 테이블 생성
-- 2. detail_categories 테이블 생성
-- 3. user_detail_category_preferences 테이블 생성
-- 4. users 테이블 수정 (department_id 추가, dark_mode 삭제)

-- 1. departments 테이블 생성 (학과 정보)
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    college VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 학과 인덱스
CREATE INDEX IF NOT EXISTS idx_departments_name ON departments(name);
CREATE INDEX IF NOT EXISTS idx_departments_college ON departments(college);

-- 2. detail_categories 테이블 생성 (상세 카테고리 - crawl_notice.category 값)
CREATE TABLE IF NOT EXISTS detail_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 상세 카테고리 인덱스
CREATE INDEX IF NOT EXISTS idx_detail_categories_name ON detail_categories(name);

-- 3. crawl_notice.category에서 고유값 추출하여 detail_categories에 삽입
INSERT INTO detail_categories (name)
SELECT DISTINCT category
FROM crawl_notice
WHERE category IS NOT NULL AND category != ''
ON CONFLICT (name) DO NOTHING;

-- 4. user_detail_category_preferences 테이블 생성 (사용자 상세 카테고리 구독)
CREATE TABLE IF NOT EXISTS user_detail_category_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    detail_category_id BIGINT NOT NULL REFERENCES detail_categories(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, detail_category_id)
);

-- 사용자 상세 카테고리 구독 인덱스
CREATE INDEX IF NOT EXISTS idx_user_detail_pref_user_id ON user_detail_category_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_detail_pref_category_id ON user_detail_category_preferences(detail_category_id);
CREATE INDEX IF NOT EXISTS idx_user_detail_pref_enabled ON user_detail_category_preferences(enabled);

-- 5. users 테이블에 department_id 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);

-- users.department_id 인덱스
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);

-- 6. users 테이블에서 dark_mode 컬럼 삭제
ALTER TABLE users DROP COLUMN IF EXISTS dark_mode;
