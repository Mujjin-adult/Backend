# pgAdmin 사용 가이드

인천대학교 공지사항 크롤링 시스템의 PostgreSQL 데이터베이스를 pgAdmin으로 관리하는 방법을 설명합니다.

## 목차
- [1. pgAdmin 접속](#1-pgadmin-접속)
- [2. 서버 등록](#2-서버-등록)
- [3. 데이터베이스 구조](#3-데이터베이스-구조)
- [4. 데이터 조회 방법](#4-데이터-조회-방법)
- [5. 유용한 SQL 쿼리](#5-유용한-sql-쿼리)
- [6. 데이터 내보내기](#6-데이터-내보내기)
- [7. 트러블슈팅](#7-트러블슈팅)

---

## 1. pgAdmin 접속

### 1.1 접속 정보

- **URL**: http://localhost:5050
- **이메일**: admin@admin.com
- **비밀번호**: admin123

### 1.2 접속 방법

1. 웹 브라우저를 열고 `http://localhost:5050` 접속
2. 로그인 페이지에서 위의 이메일과 비밀번호 입력
3. "Login" 버튼 클릭

> **참고**: Docker 컨테이너가 실행 중이어야 합니다.
> ```bash
> docker ps | grep pgadmin
> ```

---

## 2. 서버 등록

처음 접속 시 PostgreSQL 서버를 등록해야 합니다.

### 2.1 서버 등록 단계

1. 왼쪽 패널의 **"Servers"** 우클릭
2. **"Register" → "Server..."** 선택
3. 서버 정보 입력:

#### General 탭
```
Name: College Notices DB
```

#### Connection 탭
```
Host name/address: postgres
Port: 5432
Maintenance database: school_notices
Username: crawler
Password: crawler123

☑ Save password (체크)
```

#### Advanced 탭 (선택사항)
```
DB restriction: school_notices
```

4. **"Save"** 버튼 클릭

### 2.2 연결 확인

- 왼쪽 패널에 "College Notices DB" 서버가 나타남
- 서버 이름 옆에 초록색 점이 표시되면 연결 성공

---

## 3. 데이터베이스 구조

### 3.1 데이터베이스 탐색

```
Servers
  └─ College Notices DB
      └─ Databases
          └─ school_notices
              └─ Schemas
                  └─ public
                      └─ Tables
                          ├─ crawl_job        (크롤링 작업 정의)
                          ├─ crawl_task       (크롤링 태스크 실행 이력)
                          ├─ crawl_notice     (⭐ 크롤링된 문서)
                          ├─ host_budget      (호스트별 요청 제한)
                          └─ webhook          (웹훅 설정)
```

### 3.2 주요 테이블 설명

#### 📄 crawl_notice (크롤링된 문서)
가장 중요한 테이블로, 크롤링한 모든 공지사항이 저장됩니다.

**주요 컬럼:**
- `id`: 문서 고유 ID
- `job_id`: 연관된 크롤링 작업 ID
- `url`: 원본 URL
- `title`: 공지사항 제목
- `writer`: 작성자
- `date`: 작성일자 (예: "2025.10.31")
- `hits`: 조회수
- `category`: 카테고리
- `source`: 출처 (volunteer, job, scholarship 등)
- `extracted`: JSON 형식의 원본 데이터 (하위 호환성 유지)
- `raw`: 원본 JSON 문자열 (하위 호환성 유지)
- `fingerprint`: 중복 체크용 해시값
- `snapshot_version`: 스냅샷 버전
- `created_at`: 크롤링 시간

#### 🎯 crawl_job (크롤링 작업)
자동 크롤링 작업을 정의합니다.

**주요 컬럼:**
- `id`: 작업 ID
- `name`: 작업 이름 (예: "봉사 공지사항 크롤링")
- `priority`: 우선순위 (P1, P2, P3)
- `status`: 상태 (ACTIVE, PAUSED, CANCELLED)
- `schedule_cron`: 크론 스케줄
- `created_at`: 생성 시간

#### 📋 crawl_task (태스크 실행 이력)
개별 크롤링 실행 기록을 저장합니다.

**주요 컬럼:**
- `id`: 태스크 ID
- `job_id`: 연관된 작업 ID
- `url`: 크롤링한 URL
- `status`: 상태 (SUCCESS, FAILED, PENDING)
- `started_at`: 시작 시간
- `finished_at`: 완료 시간
- `last_error`: 에러 메시지

---

## 4. 데이터 조회 방법

### 4.1 GUI를 통한 조회

1. 왼쪽 패널에서 테이블 선택 (예: `crawl_notice`)
2. 테이블 우클릭 → **"View/Edit Data" → "All Rows"**
3. 상단의 데이터 그리드에서 데이터 확인

**필터 적용:**
- 컬럼 헤더 클릭 → "Filter" 아이콘 클릭
- 조건 입력 후 "OK"

### 4.2 Query Tool 사용

더 복잡한 쿼리를 실행하려면:

1. 왼쪽 패널에서 `school_notices` 데이터베이스 선택
2. 상단 메뉴: **Tools → Query Tool** (또는 단축키: `Alt+Shift+Q`)
3. SQL 쿼리 입력
4. **실행 버튼 (▶)** 클릭 또는 `F5`

---

## 5. 유용한 SQL 쿼리

### 5.1 전체 통계 조회

```sql
-- 테이블별 레코드 수
SELECT
    'crawl_notice' as table_name,
    COUNT(*) as total_records
FROM crawl_notice
UNION ALL
SELECT
    'crawl_job' as table_name,
    COUNT(*) as total_records
FROM crawl_job
UNION ALL
SELECT
    'crawl_task' as table_name,
    COUNT(*) as total_records
FROM crawl_task;
```

### 5.2 소스별 문서 수 확인

```sql
SELECT
    source as source,
    COUNT(*) as document_count,
    MAX(created_at) as last_crawled
FROM crawl_notice
GROUP BY source
ORDER BY document_count DESC;
```

### 5.3 최근 크롤링된 문서 조회

```sql
SELECT
    id,
    title as title,
    source as source,
    category as category,
    writer as writer,
    date as date,
    url,
    created_at
FROM crawl_notice
ORDER BY created_at DESC
LIMIT 20;
```

### 5.4 특정 키워드로 문서 검색

```sql
-- 제목에 "봉사"가 포함된 문서 검색
SELECT
    title as title,
    source as source,
    category as category,
    url,
    created_at
FROM crawl_notice
WHERE title ILIKE '%봉사%'
ORDER BY created_at DESC;
```

### 5.5 특정 기간 문서 조회

```sql
-- 최근 7일간 크롤링된 문서
SELECT
    title as title,
    source as source,
    created_at
FROM crawl_notice
WHERE created_at >= NOW() - INTERVAL '7 days'
ORDER BY created_at DESC;
```

### 5.6 작업별 성공률 확인

```sql
SELECT
    cj.name as job_name,
    COUNT(CASE WHEN ct.status = 'SUCCESS' THEN 1 END) as success_count,
    COUNT(CASE WHEN ct.status = 'FAILED' THEN 1 END) as failed_count,
    COUNT(*) as total_tasks,
    ROUND(
        COUNT(CASE WHEN ct.status = 'SUCCESS' THEN 1 END)::numeric /
        NULLIF(COUNT(*), 0) * 100,
        2
    ) as success_rate
FROM crawl_job cj
LEFT JOIN crawl_task ct ON cj.id = ct.job_id
GROUP BY cj.id, cj.name
ORDER BY success_rate DESC;
```

### 5.7 카테고리별 문서 수

```sql
SELECT
    category as category,
    COUNT(*) as count
FROM crawl_notice
WHERE category IS NOT NULL
GROUP BY category
ORDER BY count DESC;
```

### 5.8 중복 URL 확인

```sql
-- 중복된 URL이 있는지 확인
SELECT
    url,
    COUNT(*) as duplicate_count
FROM crawl_notice
GROUP BY url
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC;
```

### 5.9 일별 크롤링 통계

```sql
SELECT
    DATE(created_at) as crawl_date,
    COUNT(*) as documents_count,
    COUNT(DISTINCT source) as unique_sources
FROM crawl_notice
GROUP BY DATE(created_at)
ORDER BY crawl_date DESC;
```

### 5.10 가장 많이 크롤링된 작성자

```sql
SELECT
    writer as writer,
    COUNT(*) as document_count
FROM crawl_notice
WHERE writer IS NOT NULL
GROUP BY writer
ORDER BY document_count DESC
LIMIT 10;
```

---

## 6. 데이터 내보내기

### 6.1 CSV로 내보내기

1. Query Tool에서 쿼리 실행
2. 결과 그리드 상단의 **"Download as CSV (F8)"** 버튼 클릭
3. 파일 저장 위치 선택

### 6.2 전체 테이블 백업

1. 왼쪽 패널에서 테이블 우클릭
2. **"Backup..."** 선택
3. 포맷 선택 (Plain, Custom, Tar 등)
4. 파일명 지정 후 **"Backup"** 클릭

### 6.3 데이터베이스 전체 백업

1. 왼쪽 패널에서 `school_notices` 데이터베이스 우클릭
2. **"Backup..."** 선택
3. 포맷: **Custom** 또는 **Plain**
4. 경로 및 파일명 지정
5. **"Backup"** 클릭

**CLI를 통한 백업 (권장):**
```bash
# Docker 컨테이너에서 백업
docker exec college_noti-postgres-1 pg_dump -U crawler school_notices > backup_$(date +%Y%m%d).sql

# 백업 복원
docker exec -i college_noti-postgres-1 psql -U crawler school_notices < backup_20251031.sql
```

---

## 7. 트러블슈팅

### 7.1 서버에 연결할 수 없습니다

**증상:**
- "Unable to connect to server" 에러
- 서버 이름 옆에 빨간 X 표시

**해결 방법:**

1. **Docker 컨테이너 확인:**
   ```bash
   docker ps | grep postgres
   docker ps | grep pgadmin
   ```

2. **컨테이너 재시작:**
   ```bash
   docker-compose restart postgres
   docker-compose restart pgadmin
   ```

3. **Host name 확인:**
   - Docker 네트워크 내부에서는 `postgres` 사용
   - 호스트 머신에서는 `localhost` 사용

### 7.2 비밀번호 오류

**증상:**
- "password authentication failed" 에러

**해결 방법:**
- Username: `crawler` (소문자)
- Password: `crawler123`
- "Save password" 체크 확인

### 7.3 데이터가 보이지 않습니다

**확인 사항:**

1. **테이블 새로고침:**
   - 테이블 우클릭 → "Refresh"

2. **데이터 존재 여부 확인:**
   ```sql
   SELECT COUNT(*) FROM crawl_notice;
   ```

3. **크롤링 실행:**
   - 대시보드에서 "전체 크롤링 실행" 버튼 클릭
   - 또는 API 호출:
     ```bash
     curl -X POST -H "X-API-Key: dev-api-key-12345" \
       http://localhost:8000/run-crawler/all
     ```

### 7.4 Query Tool이 느립니다

**해결 방법:**

1. **LIMIT 사용:**
   ```sql
   SELECT * FROM crawl_notice LIMIT 100;
   ```

2. **인덱스 확인:**
   ```sql
   SELECT tablename, indexname
   FROM pg_indexes
   WHERE schemaname = 'public';
   ```

3. **EXPLAIN 사용 (쿼리 최적화):**
   ```sql
   EXPLAIN ANALYZE
   SELECT * FROM crawl_notice
   WHERE source = 'volunteer';
   ```

### 7.5 JSON 필드가 깨져 보입니다

**해결 방법:**

pgAdmin의 Query Tool에서 JSON을 보기 좋게 표시:

```sql
-- JSON을 보기 좋게 포맷팅
SELECT
    id,
    jsonb_pretty(extracted::jsonb) as formatted_data
FROM crawl_notice
LIMIT 5;
```

---

## 8. 추가 팁

### 8.1 즐겨찾기 추가

자주 사용하는 쿼리를 즐겨찾기에 추가:

1. Query Tool에서 쿼리 작성
2. 상단의 **"Save"** 아이콘 클릭
3. 쿼리 이름 지정
4. 왼쪽 패널 "Macros"에서 재사용 가능

### 8.2 단축키

- `F5`: 쿼리 실행
- `F7`: 쿼리 설명 (EXPLAIN)
- `F8`: CSV로 다운로드
- `Ctrl + Space`: 자동완성
- `Ctrl + Shift + C`: 주석 처리
- `Alt + Shift + Q`: Query Tool 열기

### 8.3 데이터 편집

GUI에서 직접 데이터 수정:

1. 테이블 → "View/Edit Data" → "All Rows"
2. 셀 더블클릭하여 편집
3. 상단 **"Save"** 버튼 클릭

> **주의**: 프로덕션 환경에서는 GUI 편집보다 SQL 사용 권장

---

## 9. 관련 링크

- [pgAdmin 공식 문서](https://www.pgadmin.org/docs/)
- [PostgreSQL JSON 함수](https://www.postgresql.org/docs/current/functions-json.html)
- [프로젝트 API 문서](http://localhost:8000/docs)
- [대시보드](http://localhost:8000/dashboard)

---

## 10. 문의

문제가 발생하거나 도움이 필요한 경우:

1. **로그 확인:**
   ```bash
   docker logs college_noti-postgres-1 --tail 50
   docker logs college_noti-pgadmin-1 --tail 50
   ```

2. **데이터베이스 연결 테스트:**
   ```bash
   docker exec -it college_noti-postgres-1 psql -U crawler -d school_notices
   ```

3. **이슈 리포팅:**
   - GitHub Issues
   - 프로젝트 관리자에게 문의

---

**작성일**: 2025-10-31
**버전**: 1.0
**작성자**: Claude Code Assistant
