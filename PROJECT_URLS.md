# 🌐 인천대학교 공지사항 앱 - 접속 URL 및 포트

> 🔄 **최근 업데이트**: 크롤링 서버가 메인 서버 데이터베이스로 통합되었습니다. (2025-11-03)

## 📌 메인 서비스 (메인 docker-compose.yml)

| 서비스 | URL | 포트 | 설명 |
|---|---|---|---|
| **Spring Boot API** | http://localhost:8080 | 8080 | 메인 백엔드 API 서버 |
| **Swagger UI** | http://localhost:8080/swagger-ui/index.html | 8080 | Spring Boot API 문서 |
| **PostgreSQL** | localhost:5432 | 5432 | **통합 데이터베이스** (메인 + 크롤러) |
| **Redis** | localhost:6379 | 6379 | **통합 캐시 서버** (메인 + 크롤러) |
| **pgAdmin** | http://localhost:550 | 5050 | PostgreSQL 관리 도구 |
| **Grafana** | http://localhost:3000 | 3000 | 메인 서버 모니터링 대시보드 |
| **Prometheus** | http://localhost:9090 | 9090 | 메인 서버 메트릭 수집 |
4
## 📌 크롤링 서버 (crawling-server/docker-compose.yml)

| 서비스 | URL | 포트 | 설명 |
|---|---|---|---|
| **FastAPI (크롤링)** | http://localhost:8001 | 8001 | 크롤링 API 서버 |
| **Swagger UI (크롤링)** | http://localhost:8001/docs | 8001 | 크롤링 API 문서 |
| **크롤링 대시보드** | http://localhost:8001/dashboard | 8001 | 크롤링 데이터 조회 |
| **Celery Worker** | N/A | N/A | 백그라운드 크롤링 작업 처리 |
| **Celery Beat** | N/A | N/A | 주기적 크롤링 스케줄러 |
| **Grafana (크롤러)** | http://localhost:3001 | 3001 | 크롤러 전용 모니터링 대시보드 |
| **Prometheus (크롤러)** | http://localhost:9091 | 9091 | 크롤러 전용 메트릭 수집 |

## 🔐 접속 정보

3### pgAdmin (http://localhost:5050)
- **이메일**: admin@admin.com
- **비밀번호**: admin

### Grafana (메인 서버 - http://localhost:3000)
- **사용자명**: admin
- **비밀번호**: admin

### Grafana (크롤러 - http://localhost:3001)
- **사용자명**: admin
- **비밀번호**: admin123

### PostgreSQL (localhost:5432)
- **데이터베이스**: incheon_notice (통합 데이터베이스)
- **사용자명**: postgres
- **비밀번호**: postgres
- **컨테이너명**: incheon-notice-db
- **비고**: 메인 서버와 크롤링 서버가 동일한 DB 사용

## 📊 주요 API 엔드포인트

### Spring Boot API (8080)

#### 공지사항 API
- `GET /api/notices` - 공지사항 목록 조회
- `GET /api/notices/{id}` - 공지사항 상세 조회
- `POST /api/notices/{id}/bookmark` - 공지사항 북마크 추가
- `DELETE /api/notices/{id}/bookmark` - 공지사항 북마크 제거
- `GET /api/notices/bookmarks` - 내 북마크 목록 조회

#### 카테고리 API
- `GET /api/categories` - 카테고리 목록 조회
- `GET /api/categories/{code}` - 특정 카테고리 조회
- `GET /api/categories/{code}/notices` - 카테고리별 공지사항 조회

#### 크롤러 API (내부용)
- `POST /api/crawler/notices` - 크롤링 데이터 수신 (⚠️ 더 이상 사용 안 함)
  - 크롤러가 이제 DB에 직접 저장하므로 이 API는 사용되지 않습니다

#### 시스템
- `GET /actuator/health` - 헬스 체크
- `GET /actuator/metrics` - 메트릭 조회

### 크롤링 API (8001)

#### 크롤링 실행 (API Key 필요)
- `GET /health` - 헬스 체크
- `GET /test-crawlers` - 모든 크롤러 테스트
- `POST /run-crawler/{category}` - 특정 카테고리 크롤링 실행 🔑
  - **Header**: `X-API-Key: {your-api-key}`
  - **카테고리 목록**:
    - `volunteer` - 봉사 공지사항
    - `job` - 취업 공지사항
    - `scholarship` - 장학금 공지사항
    - `general_events` - 일반행사/채용
    - `educational_test` - 교육시험
    - `tuition_payment` - 등록금납부
    - `academic_credit` - 학점
    - `degree` - 학위
    - `all` - 전체 크롤링 (8개 카테고리 모두)
  - **예시**:
    ```bash
    curl -X POST "http://localhost:8001/run-crawler/volunteer" \
      -H "X-API-Key: 0QWUQ6uNxMn4rvSqka4PeQx62ZtysZGF01VXBip0QjY"
    ```
- `POST /force-schedule-update` - Celery 스케줄 업데이트 🔑

#### 대시보드
- `GET /dashboard` - 크롤링 데이터 대시보드 (HTML)
- `GET /api/v1/health` - API v1 헬스 체크
- `GET /api/v1/metrics` - API v1 메트릭 (Prometheus)
- `GET /api/v1/crawling-status` - 크롤링 작업 상태 조회
- `GET /api/v1/documents/summary` - 문서 통계 요약
- `GET /api/v1/documents/recent` - 최근 크롤링 문서 조회

## 🚀 서비스 시작 방법

### ⚠️ 중요: 시작 순서

**반드시 메인 서비스를 먼저 시작해야 합니다** (크롤링 서버가 메인 DB/Redis 사용)

#### 1단계: 메인 서비스 시작 (PostgreSQL + Redis)
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d postgres redis pgadmin
```

#### 2단계: 크롤링 서버 시작
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app/crawling-server
docker-compose up -d
```

#### 3단계 (선택): Spring Boot 백엔드 시작
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d backend
```

### 전체 서비스 확인
```bash
# 메인 서비스 상태 확인
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose ps

# 크롤링 서버 상태 확인
cd crawling-server
docker-compose ps

# 실행 중인 모든 컨테이너 확인
docker ps
```

### 서비스 중지
```bash
# 크롤링 서버 중지
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app/crawling-server
docker-compose down

# 메인 서비스 중지
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose down
```

### 로그 확인
```bash
# Spring Boot 로그
docker logs -f incheon-notice-backend

# 크롤링 서버 로그
docker logs -f crawling-server-fastapi-1

# Celery Worker 로그
docker logs -f crawling-server-celery-worker-1
```

## 🛠️ 개발 환경 설정

### 환경 변수

#### Spring Boot (.env 또는 application.yml)
```yaml
SPRING_PROFILES_ACTIVE: dev
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/incheon_notice
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_DATA_REDIS_HOST: localhost
SPRING_DATA_REDIS_PORT: 6379
JWT_SECRET: your-super-secret-key-change-this-in-production
```

#### 크롤링 서버 (crawling-server/.env)
```bash
# 🔄 메인 서버 DB/Redis 사용 (host.docker.internal로 접근)
DATABASE_URL=postgresql://postgres:postgres@host.docker.internal:5432/incheon_notice
CELERY_BROKER_URL=redis://host.docker.internal:6379/0
CELERY_RESULT_BACKEND=redis://host.docker.internal:6379/0

# API Key (필수)
API_KEY=0QWUQ6uNxMn4rvSqka4PeQx62ZtysZGF01VXBip0QjY
SECRET_KEY=And1TeUlt-BvINEfKWmsMR-gRHnBazgAsFYunieSuakkL_N_4NP7AlXzlmNrbZshc2PveWgWaiM5ThoS0LD46w

# 크롤러 설정
DEFAULT_RATE_LIMIT_PER_HOST=1.0
MAX_CONCURRENT_REQUESTS_PER_HOST=2
MAX_REQUESTS_PER_MINUTE=60
FASTAPI_PORT=8001
```

## 📝 참고사항

### 🔄 아키텍처 변경 사항 (2025-11-03)

**변경 전 (Old)**:
```
크롤러 (FastAPI) → Spring Boot API (/api/crawler/notices) → PostgreSQL
  - 크롤러가 Spring Boot API를 통해 데이터 전송
  - 크롤러 전용 DB 사용 (school_notices)
```

**변경 후 (New - Current)**:
```
크롤러 (FastAPI + SQLAlchemy) → PostgreSQL (incheon_notice)
  - 크롤러가 메인 DB에 직접 저장
  - 단일 통합 데이터베이스 사용
  - `crawl_notice` 테이블에 저장
```

### 데이터 흐름
1. **크롤링 실행**: FastAPI `/run-crawler/{category}` 엔드포인트 호출 (API Key 필요)
2. **Celery 작업 생성**: Redis 큐에 크롤링 작업 추가
3. **Celery Worker 실행**: 백그라운드에서 크롤링 작업 처리
4. **데이터 수집**: BeautifulSoup4를 사용하여 인천대 홈페이지 크롤링
5. **데이터 저장**: SQLAlchemy를 통해 `crawl_notice` 테이블에 직접 저장
6. **중복 방지**: `fingerprint` (콘텐츠 해시) 및 `external_id` 기반 중복 체크
7. **스케줄링**: Celery Beat를 통한 주기적 자동 크롤링 (2~8시간마다)

### 데이터베이스 통합

**통합 데이터베이스**: `incheon_notice` (localhost:5432)

**주요 테이블**:
- **크롤러 테이블**:
  - `crawl_job` - 크롤링 작업 정의
  - `crawl_task` - 크롤링 태스크 실행 이력
  - `crawl_notice` - 크롤링된 공지사항 (50개 이상 저장됨 ✅)
  - `host_budget` - 호스트별 크롤링 예산 관리
  - `webhook` - 웹훅 설정

- **메인 서버 테이블**:
  - `users` - 사용자 정보
  - `categories` - 공지사항 카테고리
  - `notices` - (구) 공지사항 테이블 (향후 제거 예정)
  - `bookmarks` - 사용자 북마크
  - `notification_history` - 알림 이력
  - `user_preferences` - 사용자 설정

**FK 관계 변경**:
- `bookmarks.notice_id` → `bookmarks.crawl_notice_id`
- `notification_history.notice_id` → `notification_history.crawl_notice_id`

### 모니터링
- **메인 서버 모니터링**:
  - Grafana: http://localhost:3000
  - Prometheus: http://localhost:9090

- **크롤러 모니터링**:
  - Grafana (크롤러): http://localhost:3001
  - Prometheus (크롤러): http://localhost:9091
  - 크롤링 대시보드: http://localhost:8001/dashboard

- **데이터베이스 관리**:
  - pgAdmin: http://localhost:5050

## 🔧 트러블슈팅

### 포트 충돌 시
```bash
# 특정 포트 사용 중인 프로세스 확인
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :8080  # Spring Boot
lsof -i :8001  # 크롤링 서버

# 프로세스 종료
kill -9 <PID>

# Docker 컨테이너 확인 및 중지
docker ps | grep -E "(postgres|redis|fastapi)"
docker stop <container_name>
```

### 크롤링 서버가 DB에 연결 안 될 때
```bash
# 1. 메인 서버 PostgreSQL이 실행 중인지 확인
docker ps | grep postgres

# 2. 메인 서버 PostgreSQL 먼저 시작
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d postgres redis

# 3. 그 다음 크롤링 서버 시작
cd crawling-server
docker-compose up -d

# 4. 크롤링 서버 로그 확인
docker logs -f crawling-server-fastapi-1
```

### Docker 컨테이너 재시작
```bash
# 크롤링 서버 재시작
cd crawling-server
docker-compose restart fastapi
docker-compose restart celery-worker

# 메인 서버 재시작
cd ..
docker-compose restart backend
docker-compose restart postgres
```

### 데이터베이스 초기화
```bash
# ⚠️ 주의: 모든 데이터가 삭제됩니다!

# 1. 모든 서비스 중지 및 볼륨 삭제
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose down -v

# 2. 메인 서버 재시작
docker-compose up -d postgres redis pgadmin

# 3. 크롤링 서버 재시작
cd crawling-server
docker-compose up -d
```

### 크롤링 데이터 확인
```bash
# 데이터베이스 직접 쿼리
docker exec incheon-notice-db psql -U postgres -d incheon_notice -c "SELECT COUNT(*) FROM crawl_notice;"

# 최근 크롤링된 공지사항 5개 조회
docker exec incheon-notice-db psql -U postgres -d incheon_notice -c "SELECT id, title, source, created_at FROM crawl_notice ORDER BY created_at DESC LIMIT 5;"

# 테이블 목록 확인
docker exec incheon-notice-db psql -U postgres -d incheon_notice -c "\dt"
```

## 📚 추가 문서

- [Spring Boot API 문서](http://localhost:8080/swagger-ui/index.html) - Swagger UI
- [크롤링 API 문서](http://localhost:8001/docs) - FastAPI Swagger UI
- [크롤링 대시보드](http://localhost:8001/dashboard) - 크롤링 데이터 조회
- [Grafana 대시보드 (메인)](http://localhost:3000) - 메인 서버 모니터링
- [Grafana 대시보드 (크롤러)](http://localhost:3001) - 크롤러 모니터링
- [pgAdmin](http://localhost:5050) - PostgreSQL 관리

## 🎯 Quick Start

### 최소 구성으로 크롤링 테스트하기
```bash
# 1. 메인 DB/Redis 시작
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d postgres redis pgadmin

# 2. 크롤링 서버 시작
cd crawling-server
docker-compose up -d fastapi celery-worker

# 3. 크롤링 실행
curl -X POST "http://localhost:8001/run-crawler/volunteer" \
  -H "X-API-Key: 0QWUQ6uNxMn4rvSqka4PeQx62ZtysZGF01VXBip0QjY"

# 4. 데이터 확인 (약 10초 후)
docker exec incheon-notice-db psql -U postgres -d incheon_notice \
  -c "SELECT COUNT(*) FROM crawl_notice;"

# 5. pgAdmin에서 데이터 확인
# http://localhost:5050 접속
```

### 🎉 성공 확인
- ✅ crawl_notice 테이블에 50개 이상의 공지사항 저장됨
- ✅ 메인 서버와 크롤러가 동일한 DB 사용
- ✅ pgAdmin으로 실시간 데이터 확인 가능

---

**마지막 업데이트**: 2025-11-03
**프로젝트**: 인천대학교 공지사항 알림 앱
**개발 환경**: Docker, Spring Boot 3.2.1, FastAPI 0.120.0, PostgreSQL 16, Redis 7, Celery 5.3.4
**주요 변경사항**: 크롤링 서버 DB 통합 완료 (2025-11-03)
