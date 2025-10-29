# 🌐 인천대학교 공지사항 앱 - 접속 URL 및 포트

## 📌 메인 서비스 (메인 docker-compose.yml)

| 서비스 | URL | 포트 | 설명 |
|---|---|---|---|
| **Spring Boot API** | http://localhost:8080 | 8080 | 메인 백엔드 API 서버 |
| **Swagger UI** | http://localhost:8080/swagger-ui/index.html | 8080 | Spring Boot API 문서 |
| **PostgreSQL** | localhost:5432 | 5432 | 메인 데이터베이스 |
| **Redis** | localhost:6379 | 6379 | 캐시 서버 |
| **pgAdmin** | http://localhost:5050 | 5050 | PostgreSQL 관리 도구 |
| **Grafana** | http://localhost:3000 | 3000 | 모니터링 대시보드 |
| **Prometheus** | http://localhost:9090 | 9090 | 메트릭 수집 서버 |

## 📌 크롤링 서버 (crawling-server/docker-compose.yml)

| 서비스 | URL | 포트 | 설명 |
|---|---|---|---|
| **FastAPI (크롤링)** | http://localhost:8001 | 8001 | 크롤링 API 서버 |
| **Swagger UI (크롤링)** | http://localhost:8001/docs | 8001 | 크롤링 API 문서 |
| **크롤링 대시보드** | http://localhost:8001/dashboard | 8001 | 크롤링 데이터 조회 |
| **Celery Flower** | http://localhost:5555 | 5555 | Celery 작업 모니터링 |

## 🔐 접속 정보

### pgAdmin (http://localhost:5050)
- **이메일**: admin@admin.com
- **비밀번호**: admin

### Grafana (http://localhost:3000)
- **사용자명**: admin
- **비밀번호**: admin

### PostgreSQL (localhost:5432)
- **데이터베이스**: incheon_notice
- **사용자명**: postgres
- **비밀번호**: postgres

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
- `POST /api/crawler/notices` - 크롤링 데이터 수신 (현재 사용 안 함)

#### 시스템
- `GET /actuator/health` - 헬스 체크
- `GET /actuator/metrics` - 메트릭 조회

### 크롤링 API (8001)

#### 크롤링 실행
- `GET /health` - 헬스 체크
- `GET /test-crawlers` - 모든 크롤러 테스트
- `POST /run-crawler/{category}` - 특정 카테고리 크롤링 실행
  - **카테고리 목록**:
    - `volunteer` - 봉사
    - `job` - 취업
    - `scholarship` - 장학금
    - `general_events` - 일반행사
    - `educational_test` - 교육시험
    - `tuition_payment` - 등록금납부
    - `academic_credit` - 학점
    - `degree` - 학위
    - `all` - 전체 크롤링
- `POST /force-schedule-update` - Celery 스케줄 업데이트 (API Key 필요)

#### 대시보드
- `GET /dashboard` - 크롤링 데이터 대시보드 (HTML)
- `GET /api/v1/health` - API v1 헬스 체크
- `GET /api/v1/metrics` - API v1 메트릭

## 🚀 서비스 시작 방법

### 메인 서비스 시작
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d
```

### 크롤링 서버 시작
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app/crawling-server
docker-compose up -d
```

### 전체 서비스 확인
```bash
# 메인 서비스 상태 확인
docker-compose ps

# 크롤링 서버 상태 확인
cd crawling-server && docker-compose ps
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
# Redis
CELERY_BROKER_URL=redis://localhost:6379/1
CELERY_RESULT_BACKEND=redis://localhost:6379/1

# PostgreSQL Database
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/incheon_notice

# API Key
API_KEY=secure-crawler-key-12345

# Settings
DEFAULT_RATE_LIMIT_PER_HOST=1.0
MAX_CONCURRENT_REQUESTS_PER_HOST=2
MAX_REQUESTS_PER_MINUTE=60
```

## 📝 참고사항

### 아키텍처 변경 사항
- **크롤링 서버**는 이제 PostgreSQL에 **직접 저장**합니다
- 이전: 크롤러 → Spring Boot API → PostgreSQL
- 현재: 크롤러 → SQLAlchemy → PostgreSQL

### 데이터 흐름
1. **크롤링 실행**: FastAPI `/run-crawler/{category}` 엔드포인트 호출
2. **데이터 수집**: BeautifulSoup4를 사용하여 인천대 홈페이지 크롤링
3. **데이터 저장**: SQLAlchemy를 통해 PostgreSQL에 직접 저장
4. **중복 방지**: `external_id` (URL의 MD5 해시) 기반 UNIQUE 제약조건
5. **스케줄링**: Celery Beat를 통한 주기적 자동 크롤링

### 모니터링
- **Celery Flower** (http://localhost:5555): 크롤링 작업 실시간 모니터링
- **Grafana** (http://localhost:3000): 시스템 메트릭 시각화
- **Prometheus** (http://localhost:9090): 메트릭 수집 및 저장
- **크롤링 대시보드** (http://localhost:8001/dashboard): 크롤링 데이터 조회 및 통계

### 데이터베이스 관리
- **pgAdmin** (http://localhost:5050)을 사용하여 PostgreSQL 데이터 확인 및 관리
- Spring Boot와 크롤링 서버가 동일한 PostgreSQL 인스턴스 사용
- 데이터베이스: `incheon_notice`
- 주요 테이블:
  - `categories` - 공지사항 카테고리
  - `notices` - 공지사항
  - `bookmarks` - 사용자 북마크
  - `users` - 사용자 정보

## 🔧 트러블슈팅

### 포트 충돌 시
```bash
# 특정 포트 사용 중인 프로세스 확인
lsof -i :8080
lsof -i :8001

# 프로세스 종료
kill -9 <PID>
```

### Docker 컨테이너 재시작
```bash
# 전체 재시작
docker-compose down && docker-compose up -d

# 특정 서비스만 재시작
docker-compose restart backend
docker-compose restart fastapi
```

### 데이터베이스 초기화
```bash
# 데이터베이스 볼륨 삭제 (주의: 모든 데이터 삭제됨)
docker-compose down -v

# 재시작
docker-compose up -d
```

## 📚 추가 문서

- [Spring Boot API 문서](http://localhost:8080/swagger-ui/index.html)
- [크롤링 API 문서](http://localhost:8001/docs)
- [Celery Flower 문서](http://localhost:5555)
- [Grafana 대시보드](http://localhost:3000)

---

**마지막 업데이트**: 2025-10-29
**프로젝트**: 인천대학교 공지사항 알림 앱
**개발 환경**: Docker, Spring Boot 3.2.1, FastAPI 0.104.1, PostgreSQL 16, Redis 7
