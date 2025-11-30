# College Notice Crawler

[![Python](https://img.shields.io/badge/Python-3.12-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.104.1-009688.svg)](https://fastapi.tiangolo.com/)
[![Celery](https://img.shields.io/badge/Celery-5.3.4-37814A.svg)](https://docs.celeryproject.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

인천대학교 공지사항을 자동으로 수집하는 고성능 크롤링 시스템입니다.

## 목차

- [주요 기능](#주요-기능)
- [시스템 구성](#시스템-구성)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [아키텍처](#아키텍처)
- [API 명세](#api-명세)
- [모니터링](#모니터링)
- [개발 가이드](#개발-가이드)
- [문서](#문서)
- [트러블슈팅](#트러블슈팅)
- [성능 최적화](#성능-최적화)
- [주의사항](#주의사항)
- [기여하기](#기여하기)
- [FAQ](#faq-자주-묻는-질문)
- [문의 및 기여](#문의-및-기여)
- [로드맵](#로드맵)
- [변경 로그](#변경-로그)
- [라이센스](#라이센스)

## 주요 기능

### 크롤링 핵심 기능

**지원 카테고리 (인천대학교)**
- 🎓 **봉사 공지사항**: 사회봉사 관련 공지
- 💼 **취업 공지사항**: 진로 및 취업 정보
- 💰 **장학금 공지사항**: 장학금 신청 및 안내
- 🎉 **일반/행사 공지사항**: 학교 일반 공지 및 행사
- 📝 **교육/시험 공지사항**: 교육 프로그램 및 시험 일정
- 💳 **등록금/납부 공지사항**: 등록금 납부 관련
- 📚 **학사/학점 공지사항**: 학사 일정 및 학점 관리
- 🎓 **학위 공지사항**: 학위 취득 및 졸업 관련

**시스템 기능**
- **크롤 잡 관리**: 잡 생성/조회/수정/취소, 우선순위(P0-P3), 예약(크론/일회성) 지원
- **중복 방지**: URL canonicalization + 해시 기반 중복 체크
- **페이지네이션**: 자동 페이지 탐색 및 데이터 수집 (최대 5페이지)
- **폴리트니스 & 컴플라이언스**: robots.txt 준수, 호스트별 레이트리밋
- **데이터 저장**: 추출 결과와 원문 저장, 벌크 삽입 최적화
- **스케줄러**: Celery Beat 기반 주기적 크롤링

### 안정성 및 에러 처리

- **Circuit Breaker 패턴**: 연속 실패 방지 (CLOSED/OPEN/HALF_OPEN 상태)
- **에러 유형화**: PermanentError/TemporaryError/ValidationError 구분
- **지수 백오프 + Jitter**: 스마트 재시도 메커니즘
- **Sentry 통합**: 실시간 에러 추적 및 컨텍스트 기록
- **Pydantic 검증**: 데이터 무결성 보장

### 모니터링 및 관찰성

- **Prometheus 메트릭**: HTTP, Crawler, Circuit Breaker, DB, Celery 메트릭
- **Grafana 대시보드**: 12개 패널로 실시간 모니터링
- **Celery Flower**: 작업 큐 모니터링
- **Health Check**: 모든 서비스 상태 확인

### CI/CD 및 보안

- **GitHub Actions**: 자동 테스트, 린팅, 보안 스캔
- **자동 배포**: Staging/Production 환경 분리
- **보안 강화**: API 키 인증, CORS 설정, 환경변수 관리
- **코드 품질**: pytest, Black, isort, Flake8, Bandit

## 시스템 구성

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   FastAPI    │────▶│    Celery    │────▶│  PostgreSQL  │
│  (API 서버)   │     │  (작업 큐)    │     │  (데이터베이스)│
└──────────────┘     └──────────────┘     └──────────────┘
       │                     │                     │
       │                     │                     │
       ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Prometheus  │     │    Redis     │     │   Sentry     │
│  (메트릭 수집) │     │  (브로커/캐시) │     │ (에러 추적)   │
└──────────────┘     └──────────────┘     └──────────────┘
       │
       ▼
┌──────────────┐
│   Grafana    │
│ (시각화 도구)  │
└──────────────┘
```

### 컴포넌트

- **FastAPI**: REST API 게이트웨이/관리 UI 백엔드
- **Celery**: 분산 작업 큐 (비동기 크롤링)
- **Redis**: Celery 브로커/결과 백엔드, 캐시
- **PostgreSQL**: 메타데이터 및 크롤링 결과 저장
- **Playwright**: Headless 브라우저 (동적 렌더링)
- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 메트릭 시각화 대시보드
- **Sentry**: 실시간 에러 추적 및 성능 모니터링

## 기술 스택

### Backend
- **Python 3.12**: 최신 비동기 기능
- **FastAPI 0.104.1**: 고성능 웹 프레임워크
- **Celery 5.3.4**: 분산 작업 큐
- **SQLAlchemy 2.0.23**: ORM (비동기 지원)
- **Alembic**: 데이터베이스 마이그레이션

### Crawling
- **BeautifulSoup4**: HTML 파싱
- **Playwright**: 동적 웹 페이지 렌더링
- **httpx**: 비동기 HTTP 클라이언트

### Monitoring & Observability
- **Prometheus**: 메트릭 수집
- **Grafana**: 대시보드 시각화
- **Sentry**: 에러 추적 및 성능 모니터링
- **Celery Flower**: 작업 큐 모니터링

### Testing & Quality
- **pytest**: 테스트 프레임워워크
- **pytest-asyncio**: 비동기 테스트
- **pytest-cov**: 코드 커버리지
- **Black**: 코드 포맷터
- **isort**: Import 정렬
- **Flake8**: 린터
- **Bandit**: 보안 린터

### CI/CD
- **GitHub Actions**: 자동화된 테스트 및 배포
- **Docker**: 컨테이너화
- **Docker Compose**: 로컬 개발 환경

## 시작하기

### 사전 요구사항

- Python 3.12+
- Docker & Docker Compose
- uv (Python 패키지 관리자)

### 빠른 시작

1. **리포지토리 클론**

```bash
git clone https://github.com/Mujjin-adult/School_Notice_App.git
cd School_Notice_App/Backend/College_noti
```

2. **환경 변수 설정**

```bash
# .env.example을 복사
cp .env.example .env

# SECRET_KEY 생성 (필수)
python -c "import secrets; print(secrets.token_urlsafe(64))"

# API_KEY 생성 (필수)
python -c "import secrets; print(secrets.token_urlsafe(32))"

# .env 파일을 편집하여 생성한 키 입력
nano .env
```

**필수 설정 항목:**
- `SECRET_KEY`: 애플리케이션 암호화 키 (64자 이상)
- `API_KEY`: API 인증 키 (32자 이상)
- `DATABASE_URL`: PostgreSQL 연결 문자열
- `CELERY_BROKER_URL`: Redis 연결 문자열

자세한 환경 변수 설명은 [`.env.example`](.env.example) 파일을 참조하세요.

3. **Docker Compose로 서비스 시작**

```bash
# 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f fastapi

# 데이터베이스 마이그레이션
docker-compose exec fastapi alembic upgrade head
```

4. **서비스 접속**

- **FastAPI**: http://localhost:8000
- **API 문서 (Swagger)**: http://localhost:8000/docs
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **pgAdmin**: http://localhost:5050 (admin@admin.com/admin123)
- **Adminer**: http://localhost:8080

> ⚠️ **프로덕션 배포 시 주의사항**
>
> 프로덕션 환경에서는 반드시:
> 1. 강력한 `SECRET_KEY`와 `API_KEY` 설정
> 2. 데이터베이스 비밀번호 변경
> 3. `ENV=production`, `DEBUG=false` 설정
> 4. HTTPS 적용 및 CORS 도메인 제한
> 5. Grafana, pgAdmin, Adminer의 기본 비밀번호 변경

### 환경 변수 상세

필수 환경 변수 예시:
```env
# 데이터베이스
DATABASE_URL=postgresql://crawler:crawler123@postgres:5432/school_notices
POSTGRES_DB=school_notices
POSTGRES_USER=crawler
POSTGRES_PASSWORD=crawler123

# Redis
CELERY_BROKER_URL=redis://redis:6379/0
CELERY_RESULT_BACKEND=redis://redis:6379/0

# 보안
SECRET_KEY=your-secret-key-here
API_KEY=your-api-key-here

# 모니터링 (선택사항)
SENTRY_DSN=https://...@sentry.io/...
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin123
```

### 로컬 개발 환경

1. **의존성 설치**

```bash
cd app
uv sync
```

2. **로컬 서버 실행**

```bash
# FastAPI 서버
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Celery Worker (다른 터미널)
celery -A celery_app worker --loglevel=info

# Celery Beat (다른 터미널)
celery -A celery_app beat --loglevel=info
```

## 아키텍처

### 데이터 모델

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  CrawlJob   │──1:N─▶│  CrawlTask  │──1:N─▶│CrawlNotice  │
│  (작업 정의)  │       │  (URL 단위)  │       │  (추출 결과) │
└─────────────┘       └─────────────┘       └─────────────┘
       │
       │ 1:N
       ▼
┌─────────────┐
│   Webhook   │
│  (알림 설정)  │
└─────────────┘
```

### 주요 테이블

- **`crawl_job`**: 잡 정의 (우선순위, 스케줄, 시드 타입 등)
- **`crawl_task`**: 세부 작업/URL 단위 (상태, 재시도, 에러 등)
- **`crawl_notice`**: 추출된 공지사항 문서 (원문, 추출 결과, 스냅샷 등)
- **`host_budget`**: 호스트별 예산 관리 (QPS, 동시성, 브라우저 시간 등)
- **`webhook`**: 웹훅 설정 (잡 완료, 문서 준비, 에러 등)

### 크롤링 플로우

```
1. Job 생성 (FastAPI) → 2. Task 큐잉 (Celery) → 3. 크롤링 실행
   ↓
4. Circuit Breaker 체크 → 5. HTTP/Browser 렌더링 → 6. HTML 파싱
   ↓
7. 데이터 검증 (Pydantic) → 8. 중복 체크 → 9. 벌크 삽입 (DB)
   ↓
10. 메트릭 기록 (Prometheus) → 11. 결과 반환
```

## API 명세

### Job 관리

- **`POST /jobs`**: 크롤 잡 생성
- **`GET /jobs/{id}`**: 잡 조회
- **`GET /jobs`**: 잡 목록 조회 (필터링 및 페이징)
- **`POST /jobs/{id}/pause`**: 잡 일시정지
- **`POST /jobs/{id}/resume`**: 잡 재개
- **`POST /jobs/{id}/cancel`**: 잡 취소
- **`POST /jobs/{id}/run`**: 수동 트리거
- **`DELETE /jobs/{id}`**: 잡 삭제

### Task 관리

- **`GET /tasks`**: 태스크 목록 조회
- **`GET /tasks/{id}`**: 태스크 조회
- **`GET /jobs/{id}/tasks`**: 특정 잡의 태스크 목록

### Document 조회

- **`GET /documents`**: 문서 목록 조회 (필터링 및 페이징)
- **`GET /documents/{id}`**: 문서 조회
- **`GET /documents/search`**: 문서 검색

### 모니터링

- **`GET /health`**: 헬스 체크 (전체 서비스 상태)
- **`GET /metrics`**: Prometheus 메트릭 엔드포인트

### API 사용 예시

```bash
# 크롤 잡 생성 (장학금 공지사항)
curl -X POST "http://localhost:8000/jobs" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "장학금 공지사항 크롤링",
    "priority": "P1",
    "seed_type": "domain",
    "seed_payload": {"domain": "www.inu.ac.kr"},
    "schedule_cron": "0 9 * * *"
  }'

# 잡 상태 조회
curl -X GET "http://localhost:8000/jobs/1" \
  -H "X-API-Key: your-api-key"

# 수동 크롤 트리거
curl -X POST "http://localhost:8000/jobs/1/run" \
  -H "X-API-Key: your-api-key"

# 문서 검색 (장학금 관련)
curl -X GET "http://localhost:8000/docs?q=장학금&limit=10" \
  -H "X-API-Key: your-api-key"

# 모든 문서 조회 (페이징)
curl -X GET "http://localhost:8000/documents?skip=0&limit=20" \
  -H "X-API-Key: your-api-key"

# 헬스 체크
curl -X GET "http://localhost:8000/health"

# Prometheus 메트릭
curl -X GET "http://localhost:8000/metrics"
```

자세한 API 문서는 http://localhost:8000/docs 에서 확인하세요.

## 모니터링

### Prometheus 메트릭

시스템은 다음 메트릭을 수집합니다:

**HTTP 메트릭**
- `http_requests_total`: 총 HTTP 요청 수
- `http_request_duration_seconds`: HTTP 요청 처리 시간

**크롤러 메트릭**
- `crawler_runs_total`: 크롤러 실행 횟수 (카테고리/상태별)
- `crawler_duration_seconds`: 크롤링 소요 시간
- `crawler_items_scraped`: 수집된 아이템 수

**Circuit Breaker 메트릭**
- `circuit_breaker_state`: Circuit Breaker 상태 (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `circuit_breaker_failures_total`: 실패 횟수
- `circuit_breaker_successes_total`: 성공 횟수

**데이터베이스 메트릭**
- `db_queries_total`: 총 데이터베이스 쿼리 수
- `db_query_duration_seconds`: 쿼리 소요 시간

**Celery 메트릭**
- `celery_tasks_active`: 활성 태스크 수
- `celery_workers`: 워커 수

### Grafana 대시보드

Grafana는 자동으로 구성되며 12개 패널로 구성된 대시보드를 제공합니다:

1. HTTP Request Rate
2. HTTP Request Duration
3. Crawler Runs (Success vs Failed)
4. Items Scraped
5. Circuit Breaker State
6. Circuit Breaker Failures
7. Circuit Breaker Successes
8. Database Queries
9. Database Query Duration
10. Active Celery Tasks
11. Celery Workers
12. Crawler Errors

접속: http://localhost:3000 (admin/admin123)

### Sentry 에러 추적

Sentry는 다음 정보를 자동으로 추적합니다:

- 에러 스택 트레이스
- 요청 컨텍스트 (URL, 헤더, 바디)
- 사용자 및 세션 정보
- 성능 트랜잭션
- 크롤러 에러 컨텍스트 (카테고리, URL, 페이지 번호)

설정: `.env` 파일에 `SENTRY_DSN` 추가

## 개발 가이드

### 프로젝트 구조

```
College_noti/
├── app/
│   ├── main.py                     # FastAPI 메인 애플리케이션
│   ├── api.py                      # API 라우터 및 엔드포인트
│   ├── models.py                   # SQLAlchemy 데이터베이스 모델
│   ├── schemas.py                  # Pydantic 스키마 (요청/응답)
│   ├── crud.py                     # 데이터베이스 CRUD 작업
│   ├── database.py                 # DB 연결 및 세션 관리
│   ├── config.py                   # 설정 관리
│   ├── tasks.py                    # Celery 태스크 정의
│   ├── college_crawlers.py         # 인천대 크롤러 구현
│   ├── circuit_breaker.py          # Circuit Breaker 패턴 구현
│   ├── metrics.py                  # Prometheus 메트릭
│   ├── sentry_config.py            # Sentry 에러 추적 설정
│   ├── logging_config.py           # 로깅 설정
│   ├── robots_parser.py            # robots.txt 파서
│   ├── rate_limiter.py             # 레이트 리미터
│   ├── url_utils.py                # URL 정규화 및 중복 체크
│   ├── auto_scheduler.py           # 자동 스케줄러
│   ├── playwright_crawler.py       # Playwright 기반 동적 크롤러
│   ├── middleware/                 # 미들웨어
│   │   ├── metrics_middleware.py  # 메트릭 수집 미들웨어
│   │   ├── rate_limit_middleware.py # 레이트 리밋 미들웨어
│   │   └── security.py            # 보안 미들웨어
│   ├── tests/                      # 테스트
│   │   ├── unit/                  # 유닛 테스트
│   │   │   └── test_crud.py
│   │   └── integration/           # 통합 테스트
│   │       └── test_api_endpoints.py
│   ├── Dockerfile                  # FastAPI 컨테이너 이미지
│   └── requirements.txt            # Python 의존성
├── migrations/                     # Alembic 마이그레이션
│   └── versions/
│       └── 0001_init.py
├── .github/
│   └── workflows/                  # GitHub Actions
│       ├── ci.yml                 # CI 파이프라인
│       └── cd.yml                 # CD 파이프라인 (예정)
├── grafana/                        # Grafana 설정
│   ├── datasources.yml            # 데이터소스 설정
│   ├── dashboards.yml             # 대시보드 프로비저닝
│   └── crawler_dashboard.json     # 크롤러 대시보드
├── tests/                          # 루트 레벨 테스트
│   └── unit/
│       └── test_crawlers.py
├── prometheus.yml                  # Prometheus 설정
├── docker-compose.yml              # Docker Compose 설정
├── alembic.ini                     # Alembic 설정
├── pyproject.toml                  # Poetry 프로젝트 설정
├── .env.example                    # 환경 변수 예시
├── .gitignore                      # Git ignore 파일
├── ERROR_HANDLING.md               # 에러 처리 문서
├── SENTRY_SETUP.md                 # Sentry 설정 가이드
├── CI_CD_SETUP.md                  # CI/CD 설정 가이드
├── PGADMIN_GUIDE.md                # pgAdmin 사용 가이드
├── SECURITY.md                     # 보안 가이드
├── PROJECT_GUIDELINE.md            # 프로젝트 가이드라인
└── README.md                       # 이 파일
```

### 테스트 실행

```bash
# 전체 테스트
cd app
pytest tests/ -v

# 커버리지 포함
pytest tests/ -v --cov=. --cov-report=html

# 특정 테스트
pytest tests/test_crawlers.py::test_function_name -v
```

### 코드 품질

```bash
# 코드 포맷팅
black .

# Import 정렬
isort .

# 린팅
flake8 . --max-line-length=127

# 보안 스캔
bandit -r . -ll
```

### 새로운 크롤러 추가

인천대학교 새로운 카테고리를 크롤링하려면:

1. **`app/college_crawlers.py`에 새 메서드 추가:**

```python
def crawl_library(self, page_num: str = "123", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
    """
    도서관 공지사항 크롤링

    Args:
        page_num: 인천대 페이지 번호 (도서관 공지 페이지 ID)
        max_pages: 최대 크롤링 페이지 수

    Returns:
        추출된 공지사항 리스트
    """
    source = "library"
    url = "https://www.inu.ac.kr/bbs/getBbsRecContentList.do"

    results = []
    pages_to_crawl = max_pages or self.max_pages

    for page in range(1, pages_to_crawl + 1):
        payload = {
            "pageNum": page_num,
            "page": page,
            "pageUnit": "10"
        }

        # Circuit Breaker로 보호된 요청
        response = self._make_request_with_retry(url, payload)
        soup = BeautifulSoup(response.text, "html.parser")

        # 파싱 로직
        # ... (기존 크롤러 참고)

    return results
```

2. **`app/tasks.py`에서 Celery 태스크 분기 추가:**

```python
# college_crawl_task 함수 내에서
if job_name == "도서관 공지사항 크롤링":
    results = college_crawler.crawl_library()
```

3. **데이터베이스에 잡 등록:**

```sql
INSERT INTO crawl_job (name, status, schedule_cron)
VALUES ('도서관 공지사항 크롤링', 'ACTIVE', '0 10 * * *');
```

4. **테스트 작성** (`app/tests/test_crawlers.py`):

```python
def test_crawl_library(crawler):
    """도서관 공지사항 크롤링 테스트"""
    results = crawler.crawl_library(max_pages=1)
    assert len(results) > 0
    assert all('title' in r for r in results)
```

## 문서

- **[ERROR_HANDLING.md](ERROR_HANDLING.md)**: 에러 처리 및 Circuit Breaker 패턴 상세 가이드
- **[SENTRY_SETUP.md](SENTRY_SETUP.md)**: Sentry 에러 추적 설정 및 사용법
- **[CI_CD_SETUP.md](CI_CD_SETUP.md)**: CI/CD 파이프라인 설정 및 배포 가이드
- **[PGADMIN_GUIDE.md](PGADMIN_GUIDE.md)**: pgAdmin 사용 가이드
- **[SECURITY.md](SECURITY.md)**: 보안 가이드 및 모범 사례
- **[PROJECT_GUIDELINE.md](PROJECT_GUIDELINE.md)**: 프로젝트 개발 가이드라인

## 트러블슈팅

### 자주 발생하는 문제

#### 1. 데이터베이스 연결 실패

**증상**: `sqlalchemy.exc.OperationalError: could not connect to server`

**해결 방법**:
```bash
# PostgreSQL 컨테이너 상태 확인
docker-compose ps postgres

# 컨테이너가 실행 중이 아니면 재시작
docker-compose up -d postgres

# 로그 확인
docker-compose logs postgres

# 헬스 체크 확인
docker-compose exec postgres pg_isready -U crawler
```

#### 2. Celery Worker 작동 안 함

**증상**: 크롤링 태스크가 실행되지 않음

**해결 방법**:
```bash
# Celery Worker 로그 확인
docker-compose logs celery-worker

# Celery Worker 재시작
docker-compose restart celery-worker

# Redis 연결 확인
docker-compose exec redis redis-cli ping
# 응답: PONG

# Celery 상태 확인 (컨테이너 내부에서)
docker-compose exec celery-worker celery -A tasks inspect active
```

#### 3. 마이그레이션 에러

**증상**: `alembic.util.exc.CommandError: Target database is not up to date`

**해결 방법**:
```bash
# 현재 마이그레이션 상태 확인
docker-compose exec fastapi alembic current

# 마이그레이션 히스토리 확인
docker-compose exec fastapi alembic history

# 최신 버전으로 업그레이드
docker-compose exec fastapi alembic upgrade head

# 마이그레이션 초기화 (주의: 데이터 손실 가능)
docker-compose exec fastapi alembic downgrade base
docker-compose exec fastapi alembic upgrade head
```

#### 4. Circuit Breaker가 OPEN 상태

**증상**: `CircuitBreakerError: Circuit breaker is OPEN`

**원인**: 연속적인 실패로 Circuit Breaker가 열린 상태

**해결 방법**:
```bash
# 로그 확인하여 실패 원인 파악
docker-compose logs celery-worker | grep "Circuit breaker"

# 대상 사이트 접근 가능 여부 확인
curl -I https://www.inu.ac.kr

# Circuit Breaker는 자동으로 복구됨 (timeout 이후 HALF_OPEN 상태로 전환)
# 또는 Celery Worker 재시작
docker-compose restart celery-worker
```

#### 5. 메모리 부족

**증상**: `MemoryError` 또는 컨테이너가 계속 재시작됨

**해결 방법**:
```bash
# Docker 리소스 사용량 확인
docker stats

# Celery Worker 프로세스 수 조정 (docker-compose.yml)
# command: celery -A tasks worker --loglevel=INFO --concurrency=2

# 또는 메모리 제한 설정 (docker-compose.yml)
# mem_limit: 1g
```

#### 6. 포트 충돌

**증상**: `Error starting userland proxy: listen tcp4 0.0.0.0:8000: bind: address already in use`

**해결 방법**:
```bash
# 포트 사용 중인 프로세스 확인 (macOS/Linux)
lsof -i :8000

# 프로세스 종료
kill -9 <PID>

# 또는 .env 파일에서 포트 변경
FASTAPI_PORT=8001
```

#### 7. 크롤링 결과가 저장되지 않음

**증상**: 크롤링은 성공하지만 DB에 데이터가 없음

**해결 방법**:
```bash
# Celery Worker 로그 확인
docker-compose logs celery-worker | grep "ERROR\|Exception"

# 데이터베이스 확인 (pgAdmin 또는 Adminer 사용)
# http://localhost:5050 (pgAdmin)
# http://localhost:8080 (Adminer)

# 직접 DB 쿼리
docker-compose exec postgres psql -U crawler -d school_notices -c "SELECT COUNT(*) FROM crawl_notice;"
```

### 로그 확인

```bash
# 전체 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs fastapi
docker-compose logs celery-worker
docker-compose logs postgres

# 실시간 로그 스트리밍
docker-compose logs -f celery-worker

# 최근 100줄만 확인
docker-compose logs --tail=100 fastapi
```

### 시스템 리셋

모든 데이터를 삭제하고 처음부터 다시 시작:

```bash
# 모든 컨테이너 및 볼륨 삭제
docker-compose down -v

# Docker 이미지 재빌드
docker-compose build --no-cache

# 서비스 시작
docker-compose up -d

# 마이그레이션 실행
docker-compose exec fastapi alembic upgrade head
```

### 성능 문제

크롤링 속도가 느리거나 시스템이 느릴 때:

```bash
# 1. Prometheus 메트릭 확인
curl http://localhost:9090

# 2. Grafana 대시보드 확인
# http://localhost:3000

# 3. Celery Worker 수 증가 (docker-compose.yml)
celery-worker:
  command: celery -A tasks worker --loglevel=INFO --concurrency=4

# 4. PostgreSQL 성능 튜닝
# postgresql.conf 설정 조정 (shared_buffers, work_mem 등)
```

## 성능 최적화

### 벌크 삽입

시스템은 대량의 문서를 효율적으로 저장하기 위해 벌크 삽입을 사용합니다:

- **개선 전**: N개 문서 → N번 INSERT (N 트랜잭션)
- **개선 후**: N개 문서 → 1번 BULK INSERT (1 트랜잭션)
- **성능 향상**: 10-50배 속도 향상

### Circuit Breaker

연속 실패 시 Circuit Breaker가 자동으로 작동하여:
- 불필요한 요청 방지
- 시스템 리소스 보호
- 자동 복구 (Half-Open → Closed)

### 중복 체크 최적화

- URL 기반 중복 체크는 단일 IN 쿼리로 최적화
- 해시 기반 콘텐츠 중복 방지

## 주의사항

- **AI 파서 기능 제거됨**: 이전 버전에 있던 LLM 기반 파서 생성 기능은 제거되었습니다.
- **수동 파싱**: 각 도메인별로 수동으로 파싱 로직을 구현해야 합니다.
- **스키마 관리**: JSONSchema 기반 스키마 정의 기능은 제거되었습니다.

## 기여하기

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### PR 체크리스트

- [ ] 모든 테스트 통과
- [ ] 코드 포맷팅 (Black, isort)
- [ ] 린팅 통과 (Flake8)
- [ ] 문서 업데이트
- [ ] 커밋 메시지는 Conventional Commits 규칙 준수

## 라이센스

MIT License

## FAQ (자주 묻는 질문)

### Q1: 다른 대학 공지사항도 크롤링할 수 있나요?
A: 네, 가능합니다. `app/college_crawlers.py`에 해당 대학의 크롤러 메서드를 추가하고, `tasks.py`에서 분기 처리를 추가하면 됩니다. 각 대학의 웹사이트 구조에 맞게 파싱 로직을 작성해야 합니다.

### Q2: 크롤링 주기를 변경하려면 어떻게 하나요?
A: 데이터베이스의 `crawl_job` 테이블에서 `schedule_cron` 값을 수정하거나, API를 통해 잡을 업데이트하면 됩니다. Cron 표현식을 사용합니다 (예: `0 9 * * *` = 매일 오전 9시).

### Q3: 메모리 사용량이 높습니다.
A: `docker-compose.yml`에서 Celery Worker의 `--concurrency` 값을 줄이거나, `max_pages` 설정을 낮춰보세요. 또한 `worker_max_tasks_per_child` 설정을 통해 메모리 누수를 방지할 수 있습니다.

### Q4: Circuit Breaker가 자주 열립니다.
A: `app/college_crawlers.py`의 Circuit Breaker 설정(`failure_threshold`, `timeout`)을 조정하거나, 대상 사이트의 레이트 리밋을 확인하세요. `rate_limiter.py`에서 요청 간격을 늘릴 수 있습니다.

### Q5: 프로덕션 환경에 배포하려면?
A: [CI_CD_SETUP.md](CI_CD_SETUP.md) 문서를 참고하세요. 반드시 환경 변수를 안전하게 관리하고, HTTPS를 적용하며, 방화벽 규칙을 설정해야 합니다.

## 문의 및 기여

- **이슈 등록**: [GitHub Issues](https://github.com/Mujjin-adult/School_Notice_App/issues)
- **Pull Request**: 기여를 환영합니다! PR 체크리스트를 참고해주세요.
- **보안 취약점**: [SECURITY.md](SECURITY.md)를 참고하여 보고해주세요.

## 로드맵

- [ ] 더 많은 대학 지원 (추가 크롤러 구현)
- [ ] 실시간 알림 기능 (Slack, Discord, Email)
- [ ] 관리자 대시보드 (React 기반)
- [ ] AI 기반 공지사항 분류 및 요약
- [ ] 모바일 앱 지원 (React Native)
- [ ] GraphQL API 지원
- [ ] 다국어 지원 (i18n)

## 변경 로그

### v2.0.0 (2025-11-03)
- ✨ Circuit Breaker 패턴 추가
- ✨ Prometheus + Grafana 모니터링 추가
- ✨ Sentry 에러 추적 통합
- ✨ 벌크 삽입 최적화 (10-50배 성능 향상)
- ✨ pgAdmin, Adminer 추가
- 🐛 Celery 태스크 이름 불일치 수정
- 📝 전체 문서화 개선

### v1.0.0 (2025-09-26)
- 🎉 초기 릴리스
- ✨ 인천대학교 8개 카테고리 크롤링
- ✨ Celery Beat 기반 스케줄링
- ✨ PostgreSQL + Redis 인프라
- ✨ Docker Compose 지원

---

**프로젝트**: School Notice App - College Notice Crawler
**버전**: 2.0.0
**최종 업데이트**: 2025-11-03
**라이센스**: MIT
**개발팀**: Mujjin-adult

Made with ❤️ for students
