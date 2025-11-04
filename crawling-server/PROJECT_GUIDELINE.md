# 📘 College Notice Crawler - 프로젝트 가이드라인

> 대학교 공지사항 자동 수집 시스템의 포괄적인 기술 문서

**버전**: 1.0
**최종 업데이트**: 2025-11-03
**작성자**: Development Team

---

## 📑 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [기술 스택](#3-기술-스택)
4. [디렉토리 구조](#4-디렉토리-구조)
5. [핵심 컴포넌트](#5-핵심-컴포넌트)
6. [데이터 흐름](#6-데이터-흐름)
7. [설정 및 환경](#7-설정-및-환경)
8. [테스트 전략](#8-테스트-전략)
9. [배포 및 운영](#9-배포-및-운영)
10. [개발 가이드](#10-개발-가이드)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 목적

**College Notice Crawler**는 여러 대학교 웹사이트에서 공지사항을 자동으로 수집하고 관리하는 분산 크롤링 시스템입니다. 학생들이 여러 웹사이트를 일일이 확인하지 않고도 중요한 공지사항을 놓치지 않도록 돕는 것이 목표입니다.

### 1.2 핵심 기능

#### 크롤링 기능
- 🕷️ **다중 대학 지원**: 여러 대학 웹사이트 동시 크롤링
- 🔄 **자동 스케줄링**: Celery Beat를 통한 주기적 실행
- 🎭 **동적 페이지 지원**: Playwright 기반 JavaScript 렌더링
- ⚡ **분산 처리**: Celery 워커를 통한 병렬 크롤링
- 🔁 **스마트 재시도**: 지수 백오프 + Jitter 알고리즘

#### 데이터 관리
- 💾 **구조화된 저장**: PostgreSQL 기반 데이터 관리
- 🔍 **중복 제거**: URL 및 content hash 기반 필터링
- 📊 **통계 추적**: 크롤링 성공률 및 작업 현황 모니터링
- 📝 **버전 관리**: 동일 공지사항의 스냅샷 관리

#### API 및 보안
- 🚀 **RESTful API**: FastAPI 기반 고성능 비동기 API
- 🔐 **API Key 인증**: 안전한 접근 제어
- 🛡️ **보안 강화**: Rate Limiting, CORS, Circuit Breaker
- 📈 **실시간 모니터링**: Prometheus + Grafana + Sentry

### 1.3 주요 사용자 시나리오

#### 시나리오 1: 정기 크롤링 설정
```
관리자 → API 호출 → Job 생성 (스케줄 설정)
       → Celery Beat → 주기적 크롤링 실행
       → 결과 저장 → 웹훅 알림
```

#### 시나리오 2: 수동 크롤링 실행
```
사용자 → Dashboard → "크롤링 실행" 버튼
      → API 호출 → Celery Task 생성
      → 실시간 상태 확인
```

#### 시나리오 3: 공지사항 조회
```
앱/웹 → API 요청 → 필터링 (카테고리, 키워드)
    → PostgreSQL 쿼리 → JSON 응답
```

---

## 2. 시스템 아키텍처

### 2.1 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │Web Client│  │Mobile App│  │  cURL    │  │ Postman  │       │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘       │
│        └──────────────┴─────────────┴─────────────┘             │
│                           │ HTTP/HTTPS                           │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                   Presentation Layer                              │
│                           │                                       │
│  ┌────────────────────────▼────────────────────────┐            │
│  │           FastAPI Application                    │            │
│  │  ┌──────────────────────────────────────────┐  │            │
│  │  │  Middlewares                             │  │            │
│  │  │  - Security (API Key, CORS, Trusted)    │  │            │
│  │  │  - Rate Limiting (Redis-based)           │  │            │
│  │  │  - Metrics (Prometheus)                  │  │            │
│  │  └──────────────────────────────────────────┘  │            │
│  │                                                  │            │
│  │  ┌──────────────────────────────────────────┐  │            │
│  │  │  API Routers                             │  │            │
│  │  │  - /jobs      (Job 관리)                │  │            │
│  │  │  - /tasks     (Task 조회)               │  │            │
│  │  │  - /documents (공지사항 조회)           │  │            │
│  │  │  - /health    (헬스 체크)               │  │            │
│  │  └──────────────────────────────────────────┘  │            │
│  └──────────────────────────────────────────────┘              │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                   Business Logic Layer                            │
│                           │                                       │
│  ┌────────────────────────▼────────────────────────┐            │
│  │  CRUD Operations (crud.py)                      │            │
│  │  - Job CRUD                                      │            │
│  │  - Task CRUD                                     │            │
│  │  - Document CRUD                                 │            │
│  │  - Statistics                                    │            │
│  └────────────────────────┬────────────────────────┘            │
│                            │                                      │
│  ┌────────────────────────▼────────────────────────┐            │
│  │  College Crawlers (college_crawlers.py)        │            │
│  │  - BaseCrawler (추상 클래스)                   │            │
│  │  - KonkukCrawler                                 │            │
│  │  - SeoulTechCrawler                              │            │
│  │  - ... (확장 가능)                              │            │
│  └────────────────────────┬────────────────────────┘            │
│                            │                                      │
│  ┌────────────────────────▼────────────────────────┐            │
│  │  Resilience Patterns                            │            │
│  │  - Circuit Breaker (circuit_breaker.py)        │            │
│  │  - Rate Limiter (rate_limiter.py)              │            │
│  │  - Retry Logic (exponential backoff)           │            │
│  └─────────────────────────────────────────────────┘            │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                    Data Access Layer                              │
│                           │                                       │
│  ┌────────────────────────▼────────────────────────┐            │
│  │  SQLAlchemy ORM (models.py, database.py)       │            │
│  │  - Session Management                           │            │
│  │  - Connection Pool                               │            │
│  │  - Transaction Management                        │            │
│  └────────────────────────┬────────────────────────┘            │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                      Data Layer                                   │
│  ┌────────────────────────▼────────────────────────┐            │
│  │         PostgreSQL Database                      │            │
│  │  Tables:                                         │            │
│  │  - crawl_job     (크롤링 작업 정의)            │            │
│  │  - crawl_task    (개별 크롤링 태스크)          │            │
│  │  - crawl_notice  (수집된 공지사항)             │            │
│  │  - webhook       (이벤트 알림)                 │            │
│  └──────────────────────────────────────────────────┘            │
└───────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│                    Task Queue Layer                                │
│                                                                    │
│  ┌────────────────┐         ┌─────────────────┐                  │
│  │  Celery Beat   │────────▶│  Redis Broker   │                  │
│  │  (Scheduler)   │         │  (Message Queue)│                  │
│  └────────────────┘         └────────┬────────┘                  │
│                                       │                            │
│                             ┌─────────▼─────────┐                 │
│                             │  Celery Workers   │                 │
│                             │  (tasks.py)       │                 │
│                             │  - crawl_task     │                 │
│                             │  - process_task   │                 │
│                             └───────────────────┘                 │
└───────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│                  Monitoring & Observability                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                  │
│  │ Prometheus │  │  Grafana   │  │   Sentry   │                  │
│  │ (Metrics)  │  │(Dashboard) │  │  (Errors)  │                  │
│  └────────────┘  └────────────┘  └────────────┘                  │
└───────────────────────────────────────────────────────────────────┘
```

### 2.2 컴포넌트 간 상호작용

#### 크롤링 작업 실행 흐름
```
1. API Request (POST /jobs)
   │
   ▼
2. FastAPI Router (main.py)
   │
   ▼
3. Validation (Pydantic Schema)
   │
   ▼
4. CRUD Operation (crud.create_job)
   │
   ▼
5. Database (INSERT into crawl_job)
   │
   ▼
6. Celery Task Trigger (tasks.crawl_task.delay)
   │
   ▼
7. Redis Queue (Celery Broker)
   │
   ▼
8. Celery Worker Picks Up Task
   │
   ▼
9. Execute Crawler (college_crawlers.py)
   │
   ▼
10. Fetch Website (Playwright/Requests)
   │
   ▼
11. Parse HTML (BeautifulSoup)
   │
   ▼
12. Save Results (CRUD → PostgreSQL)
   │
   ▼
13. Update Task Status
   │
   ▼
14. Send Webhook (Optional)
```

---

## 3. 기술 스택

### 3.1 Backend Framework
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **FastAPI** | 0.104.1 | 웹 프레임워크 | 고성능 비동기, 자동 API 문서화, Type hints 지원 |
| **Uvicorn** | 0.24.0 | ASGI 서버 | FastAPI와 최적 호환, 높은 처리량 |
| **Pydantic** | 2.5.0 | 데이터 검증 | 강력한 타입 검증, 설정 관리 |

### 3.2 Database
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **PostgreSQL** | 15+ | 관계형 DB | ACID 보장, JSON 지원, 확장성 |
| **SQLAlchemy** | 2.0.23 | ORM | Python 표준, 강력한 쿼리 빌더 |
| **Alembic** | 1.13.1 | 마이그레이션 | SQLAlchemy 통합, 버전 관리 |

### 3.3 Task Queue & Cache
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **Celery** | 5.3.4 | 분산 작업 큐 | 스케줄링, 재시도, 모니터링 |
| **Redis** | 5.0.1 | 메시지 브로커/캐시 | 빠른 속도, Celery 호환 |

### 3.4 Crawling
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **Playwright** | 1.40.0 | 헤드리스 브라우저 | JavaScript 렌더링, 안정성 |
| **BeautifulSoup4** | 4.12.2 | HTML 파싱 | 간편한 API, 강력한 선택자 |
| **Requests** | 2.31.0 | HTTP 클라이언트 | 정적 페이지 크롤링 |
| **lxml** | 4.9.3 | XML/HTML 파서 | 빠른 파싱 속도 |

### 3.5 Monitoring & Logging
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **Prometheus** | - | 메트릭 수집 | 시계열 데이터, Grafana 통합 |
| **Grafana** | - | 대시보드 | 시각화, 알림 |
| **Sentry** | 2.34.1 | 에러 추적 | 실시간 에러 모니터링, 컨텍스트 |

### 3.6 Testing
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **Pytest** | 7.4.3 | 테스트 프레임워크 | 강력한 fixture, 플러그인 |
| **Pytest-cov** | 4.1.0 | 코드 커버리지 | 테스트 품질 측정 |
| **Pytest-asyncio** | 0.21.1 | 비동기 테스트 | FastAPI 테스트 |
| **httpx** | 0.25.2 | HTTP 클라이언트 | TestClient 지원 |

### 3.7 Development Tools
| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|-----------|
| **Black** | 23.12.1 | 코드 포맷터 | 일관된 스타일 |
| **isort** | 5.13.2 | Import 정렬 | 가독성 향상 |
| **flake8** | 6.1.0 | 린터 | 코드 품질 검사 |
| **mypy** | 1.7.1 | 타입 체커 | 타입 안정성 |

---

## 4. 디렉토리 구조

### 4.1 전체 구조

```
College_noti/
│
├── app/                          # 애플리케이션 루트
│   │
│   ├── main.py                   # FastAPI 앱 진입점 ⭐
│   ├── api.py                    # API 라우터 모음
│   ├── config.py                 # 설정 관리 (Pydantic Settings) ⭐
│   ├── models.py                 # SQLAlchemy 모델 ⭐
│   ├── schemas.py                # Pydantic 스키마
│   ├── crud.py                   # CRUD 작업 ⭐
│   ├── database.py               # DB 연결 및 세션
│   ├── tasks.py                  # Celery 작업 ⭐
│   │
│   ├── college_crawlers.py       # 크롤러 구현 ⭐
│   ├── playwright_crawler.py     # Playwright 래퍼
│   ├── auto_scheduler.py         # 자동 스케줄러
│   │
│   ├── circuit_breaker.py        # Circuit Breaker 패턴 ⭐
│   ├── rate_limiter.py           # Rate Limiter
│   ├── robots_parser.py          # robots.txt 파서
│   ├── url_utils.py              # URL 유틸리티
│   │
│   ├── middleware/               # FastAPI 미들웨어
│   │   ├── __init__.py
│   │   ├── security.py           # 보안 미들웨어 ⭐
│   │   ├── rate_limit_middleware.py  # Rate Limiting ⭐
│   │   └── metrics_middleware.py     # Metrics 수집
│   │
│   ├── metrics.py                # Prometheus 메트릭
│   ├── logging_config.py         # 로깅 설정
│   ├── sentry_config.py          # Sentry 설정
│   ├── slack_notify.py           # Slack 알림
│   │
│   ├── tests/                    # 테스트
│   │   ├── unit/                 # 단위 테스트
│   │   │   └── test_crud.py      # CRUD 테스트 (21개)
│   │   └── integration/          # 통합 테스트
│   │       └── test_api_endpoints.py  # API 테스트 (27개)
│   │
│   ├── .env.example              # 환경 변수 템플릿 ⭐
│   ├── requirements.txt          # Python 의존성
│   ├── Dockerfile                # Docker 이미지
│   └── worker_app.py             # Celery Worker 진입점
│
├── docker-compose.yml            # Docker Compose 설정
├── prometheus.yml                # Prometheus 설정
├── grafana/                      # Grafana 설정
│   └── dashboards/
│
├── README.md                     # 프로젝트 README
└── PROJECT_GUIDELINE.md          # 이 문서
```

⭐ = 핵심 파일

### 4.2 파일별 역할 요약

| 파일 | 라인 수 | 주요 역할 | 의존성 |
|------|---------|-----------|--------|
| `main.py` | ~580 | FastAPI 앱 생성, 미들웨어 등록, 라우터 등록 | api, config, middleware |
| `api.py` | ~620 | API 엔드포인트 정의 (/jobs, /tasks, /documents) | crud, schemas, models |
| `models.py` | ~100 | SQLAlchemy ORM 모델 (CrawlJob, CrawlTask, CrawlNotice) | database |
| `crud.py` | ~400 | 데이터베이스 CRUD 작업 | models, database |
| `tasks.py` | ~516 | Celery 비동기 작업 (크롤링, 스케줄링) | college_crawlers, crud |
| `college_crawlers.py` | ~432 | 대학별 크롤러 구현 | playwright, beautifulsoup4 |
| `config.py` | ~358 | 설정 관리 및 검증 | pydantic-settings |
| `circuit_breaker.py` | ~298 | Circuit Breaker 패턴 구현 | - |

---

## 5. 핵심 컴포넌트

### 5.1 API Layer

#### 5.1.1 FastAPI 애플리케이션 (main.py)

**역할**: 애플리케이션 진입점, 미들웨어 설정, 라우터 등록

**주요 코드 구조**:
```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware

# 앱 생성
app = FastAPI(
    title="College Notice Crawler API",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS 미들웨어
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.security.allowed_origins.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Rate Limiting 미들웨어
from middleware.rate_limit_middleware import create_rate_limit_middleware
create_rate_limit_middleware(app)

# Security 미들웨어
from middleware.security import SecurityMiddleware
app.add_middleware(SecurityMiddleware)

# 라우터 등록
from api import router
app.include_router(router)
```

**주요 엔드포인트**:
| 엔드포인트 | 메서드 | 설명 | 인증 필요 |
|-----------|--------|------|-----------|
| `/health` | GET | 헬스 체크 | ❌ |
| `/jobs` | GET | Job 목록 조회 | ✅ |
| `/jobs` | POST | Job 생성 | ✅ |
| `/jobs/{id}` | GET | Job 조회 | ✅ |
| `/jobs/{id}` | DELETE | Job 삭제 | ✅ |
| `/jobs/{id}/pause` | POST | Job 일시정지 | ✅ |
| `/tasks` | GET | Task 목록 조회 | ✅ |
| `/documents` | GET | 공지사항 목록 조회 | ✅ |
| `/documents/search` | GET | 공지사항 검색 | ✅ |

#### 5.1.2 API 라우터 (api.py)

**Job 관리 예시**:
```python
@router.post("/jobs", response_model=JobResponse)
async def create_job(
    job_data: JobCreate,
    db: Session = Depends(get_db),
    api_key: str = Depends(verify_api_key)
):
    """새로운 크롤링 작업 생성"""

    # 중복 체크
    existing = crud.get_job_by_name(db, job_data.name)
    if existing:
        raise HTTPException(400, "Job already exists")

    # Job 생성
    job = crud.create_job(db, job_data.dict())

    # Celery 작업 예약
    if job_data.schedule_type == "cron":
        schedule_cron_job(job.id, job_data.schedule_cron)

    return job
```

**문서 검색 예시**:
```python
@router.get("/documents/search")
async def search_documents(
    q: str,
    category: Optional[str] = None,
    skip: int = 0,
    limit: int = 20,
    db: Session = Depends(get_db),
    api_key: str = Depends(verify_api_key)
):
    """공지사항 검색 (제목, 내용 전문 검색)"""
    results = crud.search_documents(
        db, q=q, category=category, skip=skip, limit=limit
    )
    return results
```

### 5.2 데이터 모델 (models.py)

#### 5.2.1 CrawlJob (크롤링 작업)

```python
class CrawlJob(Base):
    __tablename__ = "crawl_job"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(128), nullable=False, unique=True, index=True)
    priority = Column(String(4), nullable=False, index=True)  # P0-P3
    schedule_cron = Column(String(64), nullable=True)
    seed_type = Column(Enum(SeedType), nullable=False)  # URL_LIST, SITEMAP, DOMAIN
    seed_payload = Column(JSON, nullable=False)  # {"urls": [...]}
    render_mode = Column(Enum(RenderMode), nullable=False)  # STATIC, HEADLESS
    rate_limit_per_host = Column(Float, default=1.0)
    max_depth = Column(Integer, default=1)
    robots_policy = Column(Enum(RobotsPolicy), nullable=False)  # OBEY, IGNORE
    status = Column(Enum(JobStatus), default=JobStatus.ACTIVE)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # 관계
    tasks = relationship("CrawlTask", back_populates="job")
    documents = relationship("CrawlNotice", back_populates="job")
```

**주요 필드 설명**:
- `seed_type`: 크롤링 시작점 타입
  - `URL_LIST`: URL 리스트
  - `SITEMAP`: sitemap.xml 기반
  - `DOMAIN`: 도메인 전체 탐색
- `render_mode`: 렌더링 방식
  - `STATIC`: HTTP 요청 (빠름)
  - `HEADLESS`: Playwright 브라우저 (JavaScript 지원)
- `robots_policy`: robots.txt 준수 정책

#### 5.2.2 CrawlTask (개별 크롤링 태스크)

```python
class CrawlTask(Base):
    __tablename__ = "crawl_task"

    id = Column(Integer, primary_key=True, index=True)
    job_id = Column(Integer, ForeignKey("crawl_job.id"), nullable=False)
    url = Column(Text, nullable=False, index=True)
    status = Column(Enum(TaskStatus), default=TaskStatus.PENDING)  # PENDING, SUCCESS, FAILED
    retries = Column(Integer, default=0)
    last_error = Column(Text, nullable=True)

    started_at = Column(DateTime(timezone=True), nullable=True)
    finished_at = Column(DateTime(timezone=True), nullable=True)

    http_status = Column(Integer, nullable=True)
    content_hash = Column(String(64), nullable=True, index=True)
    blocked_flag = Column(Boolean, default=False)
    cost_ms_browser = Column(Integer, nullable=True)

    # 관계
    job = relationship("CrawlJob", back_populates="tasks")
```

**상태 전이 다이어그램**:
```
PENDING ──────► RUNNING ──────► SUCCESS
   │               │
   │               │
   │               ▼
   └──────────► FAILED ──────► RETRY (max 3회)
                  │
                  ▼
              ABANDONED (재시도 초과)
```

#### 5.2.3 CrawlNotice (수집된 공지사항)

```python
class CrawlNotice(Base):
    __tablename__ = "crawl_notice"

    id = Column(Integer, primary_key=True, index=True)
    job_id = Column(Integer, ForeignKey("crawl_job.id"), nullable=False)

    url = Column(Text, nullable=False, index=True)
    title = Column(Text, nullable=True)
    writer = Column(String(128), nullable=True)
    date = Column(String(32), nullable=True)
    category = Column(String(64), nullable=True, index=True)
    source = Column(String(64), nullable=True)

    extracted = Column(JSON, nullable=True)  # 추출된 구조화 데이터
    raw = Column(Text, nullable=True)  # 원본 HTML

    fingerprint = Column(String(64), nullable=False, unique=True, index=True)
    snapshot_version = Column(String(32), nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # 관계
    job = relationship("CrawlJob", back_populates="documents")
```

**fingerprint 생성 로직**:
```python
import hashlib

def generate_fingerprint(url: str, content: str) -> str:
    """URL + 컨텐츠 기반 고유 지문 생성 (중복 방지)"""
    data = f"{url}:{content}".encode('utf-8')
    return hashlib.sha256(data).hexdigest()
```

### 5.3 비즈니스 로직 (crud.py)

#### 5.3.1 Job CRUD

```python
def create_job(db: Session, job_data: Dict[str, Any]) -> CrawlJob:
    """새로운 크롤 잡 생성"""
    db_job = CrawlJob(**job_data)
    db.add(db_job)
    db.commit()
    db.refresh(db_job)
    return db_job

def get_job(db: Session, job_id: int) -> Optional[CrawlJob]:
    """ID로 잡 조회"""
    return db.query(CrawlJob).filter(CrawlJob.id == job_id).first()

def update_job_status(db: Session, job_id: int, status: JobStatus) -> CrawlJob:
    """잡 상태 업데이트"""
    job = get_job(db, job_id)
    if not job:
        raise ValueError(f"Job {job_id} not found")

    job.status = status
    job.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(job)
    return job
```

#### 5.3.2 Document CRUD (벌크 최적화)

```python
def bulk_create_documents(db: Session, docs_data: List[Dict]) -> int:
    """문서 벌크 삽입 (성능 최적화)"""
    if not docs_data:
        return 0

    # 벌크 insert를 위한 ORM 객체 리스트
    documents = [CrawlNotice(**doc) for doc in docs_data]

    try:
        db.bulk_save_objects(documents)
        db.commit()
        return len(documents)
    except IntegrityError as e:
        # 중복 fingerprint 처리
        db.rollback()
        logger.warning(f"Duplicate documents found: {e}")
        return 0
```

**성능 비교**:
- 개별 insert: 100개 문서 → ~5초
- 벌크 insert: 100개 문서 → **~0.5초** (10배 빠름)

#### 5.3.3 통계 집계

```python
def get_job_statistics(db: Session, job_id: int) -> Dict[str, Any]:
    """잡 통계 집계"""
    tasks = db.query(CrawlTask).filter(CrawlTask.job_id == job_id).all()

    total = len(tasks)
    completed = sum(1 for t in tasks if t.status == TaskStatus.SUCCESS)
    failed = sum(1 for t in tasks if t.status == TaskStatus.FAILED)

    return {
        "total_tasks": total,
        "completed_tasks": completed,
        "failed_tasks": failed,
        "success_rate": (completed / total * 100) if total > 0 else 0,
        "avg_response_time": sum(t.cost_ms_browser or 0 for t in tasks) / total if total > 0 else 0
    }
```

### 5.4 비동기 작업 (tasks.py)

#### 5.4.1 Celery 설정

```python
from celery import Celery
from config import get_settings

settings = get_settings()

celery_app = Celery(
    "college_crawler",
    broker=settings.redis.broker_url,
    backend=settings.redis.result_backend
)

celery_app.conf.update(
    task_serializer='json',
    result_serializer='json',
    accept_content=['json'],
    timezone='Asia/Seoul',
    enable_utc=True,

    # 재시도 설정
    task_acks_late=True,
    task_reject_on_worker_lost=True,

    # Beat 스케줄
    beat_schedule={
        'periodic-crawl': {
            'task': 'tasks.periodic_crawl_all',
            'schedule': crontab(hour='*/6'),  # 6시간마다
        }
    }
)
```

#### 5.4.2 크롤링 작업

```python
@celery_app.task(
    bind=True,
    max_retries=3,
    default_retry_delay=60,  # 60초 후 재시도
    autoretry_for=(TemporaryError,)
)
def crawl_task(self, job_id: int, url: str):
    """개별 URL 크롤링 작업"""

    from database import SessionLocal
    from college_crawlers import get_college_crawler

    db = SessionLocal()

    try:
        # Job 조회
        job = crud.get_job(db, job_id)
        if not job:
            raise ValueError(f"Job {job_id} not found")

        # 크롤러 선택
        crawler = get_college_crawler(job.name)

        # Circuit Breaker 적용
        with CircuitBreaker(name=f"crawler_{job.name}"):
            # 크롤링 실행
            results = crawler.crawl(url)

        # 결과 저장
        saved = crud.bulk_create_documents(db, results)

        logger.info(f"Crawled {url}: {saved} documents saved")

        return {"status": "success", "documents": saved}

    except CircuitBreakerOpenError:
        # Circuit Breaker 열림 - 일시적 실패
        logger.warning(f"Circuit breaker open for {job.name}")
        raise self.retry(countdown=300)  # 5분 후 재시도

    except Exception as e:
        logger.error(f"Crawl failed for {url}: {e}")
        raise

    finally:
        db.close()
```

**재시도 전략 (Exponential Backoff + Jitter)**:
```python
def exponential_backoff_with_jitter(retry_count: int, base_delay: int = 60) -> int:
    """
    지수 백오프 + Jitter

    retry_count=0: 60s
    retry_count=1: 120s + random(0-30s)
    retry_count=2: 240s + random(0-60s)
    """
    import random

    delay = base_delay * (2 ** retry_count)
    jitter = random.randint(0, delay // 2)
    return delay + jitter
```

### 5.5 크롤러 엔진 (college_crawlers.py)

#### 5.5.1 BaseCrawler (추상 클래스)

```python
from abc import ABC, abstractmethod
from typing import List, Dict

class BaseCrawler(ABC):
    """모든 크롤러의 기본 클래스"""

    def __init__(self, rate_limit: float = 1.0):
        self.rate_limit = rate_limit  # 요청 간 최소 간격 (초)
        self.last_request_time = 0

    @abstractmethod
    async def crawl(self, url: str) -> List[Dict]:
        """
        크롤링 실행 (반드시 구현 필요)

        Returns:
            List[Dict]: 공지사항 리스트
                [
                    {
                        "title": "공지사항 제목",
                        "url": "https://...",
                        "date": "2025-11-03",
                        "category": "scholarship",
                        ...
                    }
                ]
        """
        pass

    def respect_rate_limit(self):
        """Rate Limit 준수"""
        import time

        elapsed = time.time() - self.last_request_time
        if elapsed < self.rate_limit:
            time.sleep(self.rate_limit - elapsed)

        self.last_request_time = time.time()

    async def fetch_html(self, url: str, use_browser: bool = False) -> str:
        """HTML 가져오기"""
        self.respect_rate_limit()

        if use_browser:
            # Playwright 사용 (JavaScript 렌더링)
            return await self._fetch_with_playwright(url)
        else:
            # Requests 사용 (정적 페이지)
            return self._fetch_with_requests(url)
```

#### 5.5.2 대학별 크롤러 구현 예시

```python
class KonkukCrawler(BaseCrawler):
    """건국대학교 크롤러"""

    BASE_URL = "https://www.konkuk.ac.kr"
    NOTICE_URL = f"{BASE_URL}/ko/board/notice"

    async def crawl(self, url: str) -> List[Dict]:
        """건국대 공지사항 크롤링"""

        # HTML 가져오기
        html = await self.fetch_html(url, use_browser=False)

        # BeautifulSoup으로 파싱
        soup = BeautifulSoup(html, 'lxml')

        # 공지사항 리스트 추출
        notices = []
        for item in soup.select('.board-list tr'):
            try:
                title_elem = item.select_one('.title')
                date_elem = item.select_one('.date')

                if not title_elem:
                    continue

                notice = {
                    "title": title_elem.text.strip(),
                    "url": urljoin(self.BASE_URL, title_elem['href']),
                    "date": date_elem.text.strip() if date_elem else None,
                    "category": self._extract_category(item),
                    "source": "konkuk",
                    "fingerprint": self._generate_fingerprint(title_elem['href'])
                }

                notices.append(notice)

            except Exception as e:
                logger.warning(f"Failed to parse item: {e}")
                continue

        return notices

    def _extract_category(self, item) -> str:
        """카테고리 추출 로직"""
        category_elem = item.select_one('.category')
        if category_elem:
            text = category_elem.text.strip()
            if '장학' in text:
                return 'scholarship'
            elif '학사' in text:
                return 'academic'
        return 'general'
```

**크롤러 등록**:
```python
# college_crawlers.py 하단
COLLEGE_CRAWLERS = {
    "konkuk": KonkukCrawler,
    "seoultech": SeoulTechCrawler,
    "korea": KoreaUnivCrawler,
    # ... 확장 가능
}

def get_college_crawler(name: str) -> BaseCrawler:
    """크롤러 팩토리 함수"""
    crawler_class = COLLEGE_CRAWLERS.get(name)
    if not crawler_class:
        raise ValueError(f"Unknown crawler: {name}")

    return crawler_class()
```

### 5.6 안정성 패턴

#### 5.6.1 Circuit Breaker

```python
class CircuitBreaker:
    """
    Circuit Breaker 패턴 구현

    상태:
    - CLOSED: 정상 동작
    - OPEN: 차단 (연속 실패 임계값 초과)
    - HALF_OPEN: 복구 시도
    """

    def __init__(self,
                 name: str,
                 failure_threshold: int = 5,
                 timeout: int = 60,
                 success_threshold: int = 2):
        self.name = name
        self.failure_threshold = failure_threshold
        self.timeout = timeout
        self.success_threshold = success_threshold

        self.state = "CLOSED"
        self.failure_count = 0
        self.success_count = 0
        self.last_failure_time = None

    def call(self, func, *args, **kwargs):
        """함수 실행 with Circuit Breaker"""

        if self.state == "OPEN":
            # Circuit이 열린 상태
            if time.time() - self.last_failure_time >= self.timeout:
                # 타임아웃 경과 → HALF_OPEN으로 전환
                self.state = "HALF_OPEN"
                logger.info(f"Circuit {self.name}: OPEN → HALF_OPEN")
            else:
                # 아직 차단 상태
                raise CircuitBreakerOpenError(f"Circuit {self.name} is OPEN")

        try:
            # 함수 실행
            result = func(*args, **kwargs)

            # 성공 처리
            self.on_success()

            return result

        except Exception as e:
            # 실패 처리
            self.on_failure()
            raise

    def on_success(self):
        """성공 시 처리"""
        self.failure_count = 0

        if self.state == "HALF_OPEN":
            self.success_count += 1

            if self.success_count >= self.success_threshold:
                # 복구 성공 → CLOSED
                self.state = "CLOSED"
                self.success_count = 0
                logger.info(f"Circuit {self.name}: HALF_OPEN → CLOSED")

    def on_failure(self):
        """실패 시 처리"""
        self.failure_count += 1
        self.last_failure_time = time.time()

        if self.failure_count >= self.failure_threshold:
            # 임계값 초과 → OPEN
            self.state = "OPEN"
            logger.warning(f"Circuit {self.name}: CLOSED → OPEN")
```

**사용 예시**:
```python
breaker = CircuitBreaker(name="external_api", failure_threshold=3, timeout=60)

try:
    result = breaker.call(requests.get, "https://api.example.com/data")
except CircuitBreakerOpenError:
    # Circuit이 열림 → 대체 로직
    result = get_cached_data()
```

#### 5.6.2 Rate Limiter (Token Bucket)

```python
class TokenBucketRateLimiter:
    """
    Token Bucket 알고리즘 기반 Rate Limiter

    - 초당 N개의 토큰 생성
    - 최대 M개의 토큰 보유 가능
    - 요청 시 토큰 1개 소비
    """

    def __init__(self, rate: float, capacity: int):
        """
        Args:
            rate: 초당 토큰 생성 속도
            capacity: 최대 토큰 용량
        """
        self.rate = rate
        self.capacity = capacity
        self.tokens = capacity
        self.last_update = time.time()
        self.lock = threading.Lock()

    def acquire(self, tokens: int = 1) -> bool:
        """토큰 획득 시도"""
        with self.lock:
            self._refill()

            if self.tokens >= tokens:
                self.tokens -= tokens
                return True
            else:
                return False

    def _refill(self):
        """토큰 재충전"""
        now = time.time()
        elapsed = now - self.last_update

        # 경과 시간에 비례하여 토큰 추가
        new_tokens = elapsed * self.rate
        self.tokens = min(self.capacity, self.tokens + new_tokens)

        self.last_update = now
```

**사용 예시**:
```python
# 초당 2개 요청 허용
limiter = TokenBucketRateLimiter(rate=2.0, capacity=10)

if limiter.acquire():
    # 요청 실행
    make_request()
else:
    # Rate limit 초과
    raise RateLimitExceeded("Too many requests")
```

### 5.7 보안 (middleware/security.py)

#### 5.7.1 API Key 인증

```python
from fastapi import Security, HTTPException
from fastapi.security import APIKeyHeader

api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

async def verify_api_key(api_key: str = Security(api_key_header)):
    """API 키 검증"""

    settings = get_settings()

    if not api_key:
        raise HTTPException(
            status_code=401,
            detail="API Key is required",
            headers={"WWW-Authenticate": "ApiKey"}
        )

    if api_key != settings.security.api_key:
        raise HTTPException(
            status_code=403,
            detail="Invalid API Key"
        )

    return api_key
```

**사용 예시**:
```python
@router.get("/protected")
async def protected_route(api_key: str = Depends(verify_api_key)):
    """API 키가 필요한 엔드포인트"""
    return {"message": "Access granted"}
```

#### 5.7.2 Rate Limiting Middleware

```python
class RateLimitMiddleware(BaseHTTPMiddleware):
    """Redis 기반 분산 Rate Limiting"""

    async def dispatch(self, request: Request, call_next):
        # API 키 또는 IP 주소로 클라이언트 식별
        client_id = self._get_client_id(request)

        # Rate limit 확인
        allowed, remaining, reset_time = await self._check_rate_limit(client_id)

        if not allowed:
            return JSONResponse(
                status_code=429,
                content={"error": "Rate limit exceeded"},
                headers={
                    "X-RateLimit-Limit": str(self.max_requests),
                    "X-RateLimit-Remaining": "0",
                    "X-RateLimit-Reset": str(reset_time),
                    "Retry-After": str(reset_time)
                }
            )

        # 요청 처리
        response = await call_next(request)

        # Rate limit 헤더 추가
        response.headers["X-RateLimit-Limit"] = str(self.max_requests)
        response.headers["X-RateLimit-Remaining"] = str(remaining)

        return response
```

### 5.8 모니터링 (metrics.py, sentry_config.py)

#### 5.8.1 Prometheus 메트릭

```python
from prometheus_client import Counter, Histogram, Gauge

# HTTP 요청 카운터
http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'endpoint', 'status']
)

# 응답 시간 히스토그램
http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration',
    ['method', 'endpoint']
)

# 활성 작업 게이지
active_tasks = Gauge(
    'active_crawl_tasks',
    'Number of active crawl tasks'
)

# 크롤링 성공/실패 카운터
crawl_results_total = Counter(
    'crawl_results_total',
    'Total crawl results',
    ['status', 'college']
)
```

**메트릭 수집 예시**:
```python
@router.get("/api/data")
async def get_data():
    # 요청 카운트
    http_requests_total.labels(method="GET", endpoint="/api/data", status="200").inc()

    # 응답 시간 측정
    with http_request_duration_seconds.labels(method="GET", endpoint="/api/data").time():
        data = fetch_data()

    return data
```

#### 5.8.2 Sentry 에러 추적

```python
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration
from sentry_sdk.integrations.celery import CeleryIntegration

def init_sentry():
    """Sentry 초기화"""
    settings = get_settings()

    if not settings.monitoring.sentry_dsn:
        return

    sentry_sdk.init(
        dsn=settings.monitoring.sentry_dsn,
        environment=settings.monitoring.environment,
        integrations=[
            FastApiIntegration(),
            CeleryIntegration()
        ],
        traces_sample_rate=0.1,  # 10% 트랜잭션 샘플링
        profiles_sample_rate=0.1
    )
```

**에러 컨텍스트 추가**:
```python
from sentry_sdk import capture_exception, set_context

try:
    result = crawl_website(url)
except Exception as e:
    # 컨텍스트 추가
    set_context("crawl_job", {
        "job_id": job_id,
        "url": url,
        "crawler": crawler_name
    })

    # Sentry로 전송
    capture_exception(e)

    raise
```

---

## 6. 데이터 흐름

### 6.1 크롤링 작업 생성 흐름

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ POST /jobs
     │ {
     │   "name": "konkuk_scholarship",
     │   "seed_type": "URL_LIST",
     │   "seed_payload": {"urls": [...]},
     │   "schedule_cron": "0 */6 * * *"
     │ }
     ▼
┌─────────────────────────────────────┐
│  FastAPI (main.py)                  │
│  1. API Key 검증                    │
│  2. Rate Limit 확인                 │
│  3. Pydantic 스키마 검증            │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  API Router (api.py)                │
│  1. 중복 Job 확인                   │
│  2. crud.create_job() 호출          │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  CRUD (crud.py)                     │
│  1. ORM 객체 생성                   │
│  2. DB에 INSERT                     │
│  3. commit & refresh                │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  PostgreSQL                         │
│  INSERT INTO crawl_job VALUES(...)  │
└────────────┬────────────────────────┘
             │
             │ (Job ID 반환)
             ▼
┌─────────────────────────────────────┐
│  Celery Beat Scheduler              │
│  1. cron 표현식 파싱                │
│  2. 스케줄 등록                     │
│  3. 주기적 작업 트리거              │
└─────────────────────────────────────┘
```

### 6.2 크롤링 실행 흐름

```
┌─────────────────────┐
│  Celery Beat        │
│  (매 6시간마다)     │
└──────┬──────────────┘
       │ tasks.periodic_crawl_all.delay()
       ▼
┌─────────────────────────────────────┐
│  Redis (Message Queue)              │
│  Task Queue에 메시지 추가           │
└──────┬──────────────────────────────┘
       │
       │ Worker가 메시지 획득
       ▼
┌─────────────────────────────────────┐
│  Celery Worker                      │
│  tasks.crawl_task(job_id, url)      │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  1. Job 조회 (DB)                   │
│  job = crud.get_job(db, job_id)     │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  2. 크롤러 선택                     │
│  crawler = get_college_crawler(     │
│      job.name                        │
│  )                                   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  3. Circuit Breaker 확인            │
│  with CircuitBreaker(...):          │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  4. 웹사이트 접속                   │
│  - robots.txt 확인                  │
│  - Rate Limiting 적용               │
│  - HTML 다운로드                    │
│    (Playwright or Requests)         │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  5. HTML 파싱                       │
│  soup = BeautifulSoup(html)         │
│  notices = []                        │
│  for item in soup.select(...):      │
│      notices.append({...})           │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  6. 중복 제거                       │
│  for notice in notices:              │
│      fingerprint = generate(...)     │
│      if exists(fingerprint):         │
│          skip                        │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  7. 데이터 저장 (벌크)              │
│  saved = crud.bulk_create_documents( │
│      db, notices                     │
│  )                                   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  PostgreSQL                         │
│  BULK INSERT INTO crawl_notice      │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  8. Task 상태 업데이트              │
│  crud.update_task_status(           │
│      task_id, SUCCESS               │
│  )                                   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  9. 웹훅 알림 (선택)                │
│  send_webhook(job_id, result)       │
└─────────────────────────────────────┘
```

### 6.3 공지사항 조회 흐름

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ GET /documents?category=scholarship&limit=20
     ▼
┌─────────────────────────────────────┐
│  FastAPI Middleware                 │
│  1. Rate Limit 확인                 │
│  2. API Key 검증                    │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  API Router                         │
│  crud.get_documents(                │
│      category="scholarship",         │
│      limit=20                        │
│  )                                   │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  CRUD                               │
│  query = db.query(CrawlNotice)      │
│  if category:                        │
│      query.filter(category=category)│
│  query.limit(20)                     │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  PostgreSQL                         │
│  SELECT * FROM crawl_notice         │
│  WHERE category = 'scholarship'     │
│  LIMIT 20                            │
└────────────┬────────────────────────┘
             │
             │ (결과 반환)
             ▼
┌─────────────────────────────────────┐
│  Pydantic Schema 변환               │
│  [DocumentResponse(...) for ...]    │
└────────────┬────────────────────────┘
             │
             │ JSON 응답
             ▼
┌──────────┐
│  Client  │
│  {        │
│    "data": [                         │
│      {                                │
│        "id": 1,                       │
│        "title": "...",                │
│        "url": "...",                  │
│        ...                            │
│      }                                │
│    ]                                  │
│  }        │
└──────────┘
```

---

## 7. 설정 및 환경

### 7.1 Configuration 구조 (config.py)

**계층 구조**:
```
Settings (최상위)
├── database: DatabaseSettings
│   ├── url
│   ├── pool_size
│   └── max_overflow
├── redis: RedisSettings
│   ├── host
│   ├── port
│   └── password
├── crawler: CrawlerSettings
│   ├── default_rate_limit_per_host
│   └── max_concurrent_requests_per_host
├── playwright: PlaywrightSettings
│   ├── browser_type
│   └── headless
├── monitoring: MonitoringSettings
│   ├── sentry_dsn
│   └── log_level
├── notification: NotificationSettings
│   └── slack_webhook_url
└── security: SecuritySettings
    ├── api_key
    ├── allowed_origins
    └── enable_rate_limiting
```

**설정 로딩 순서**:
```
1. 기본값 (config.py의 Field default)
   ↓
2. .env 파일
   ↓
3. 환경 변수 (우선순위 높음)
   ↓
4. validate_settings() 검증
   ↓
5. 애플리케이션에서 사용
```

### 7.2 환경 변수 우선순위

**프로덕션 vs 개발 환경**:

| 항목 | 개발 환경 | 프로덕션 환경 |
|------|-----------|---------------|
| `ENV` | development | production |
| `DEBUG` | true | false |
| `SECRET_KEY` | 32자+ | **64자+ 필수** |
| `API_KEY` | 16자+ | **32자+ 필수** |
| `DATABASE_URL` | crawler123 허용 | **강력한 비밀번호 필수** |
| `LOG_LEVEL` | DEBUG | WARNING/ERROR |
| `SENTRY_DSN` | 선택 | **필수** |
| `CORS` | * 허용 | 특정 도메인만 |

**검증 로직** (config.py:250-351):
```python
def validate_settings():
    """설정 유효성 검사"""
    settings = get_settings()

    # SECRET_KEY 검증
    if settings.monitoring.environment == "production":
        if len(settings.secret_key) < 64:
            raise ValueError("❌ SECRET_KEY must be at least 64 characters in production")

        # 취약한 키 감지
        insecure_keys = ["your-secret-key-here", "dev-secret-key", "test", "password"]
        if any(weak in settings.secret_key.lower() for weak in insecure_keys):
            raise ValueError("❌ Cannot use weak SECRET_KEY in production")

        # 데이터베이스 비밀번호 검증
        weak_passwords = ["crawler123", "password", "admin", "12345"]
        db_url_lower = settings.database.url.lower()
        if any(weak in db_url_lower for weak in weak_passwords):
            raise ValueError("❌ Weak database password detected in production")
```

### 7.3 주요 환경 변수

| 환경 변수 | 설명 | 기본값 | 프로덕션 필수 |
|-----------|------|--------|---------------|
| `DATABASE_URL` | PostgreSQL 연결 문자열 | `postgresql://crawler:crawler123@postgres:5432/school_notices` | ✅ |
| `CELERY_BROKER_URL` | Redis 브로커 URL | `redis://redis:6379/0` | ✅ |
| `SECRET_KEY` | 애플리케이션 암호화 키 | - | ✅ |
| `API_KEY` | API 인증 키 | - | ✅ |
| `SENTRY_DSN` | Sentry DSN | - | ✅ |
| `LOG_LEVEL` | 로그 레벨 | `INFO` | ❌ |
| `ENABLE_RATE_LIMITING` | Rate Limiting 활성화 | `true` | ✅ |
| `MAX_REQUESTS_PER_MINUTE` | 분당 최대 요청 수 | `60` | ❌ |

---

## 8. 테스트 전략

### 8.1 테스트 구조

```
tests/
├── unit/                    # 단위 테스트
│   └── test_crud.py         # CRUD 함수 테스트 (21개)
│       ├── TestJobCRUD      # Job CRUD (7개)
│       ├── TestTaskCRUD     # Task CRUD (5개)
│       ├── TestDocumentCRUD # Document CRUD (6개)
│       ├── TestStatistics   # 통계 (1개)
│       └── TestBulkOperations (2개)
│
└── integration/             # 통합 테스트
    └── test_api_endpoints.py  # API 엔드포인트 (27개)
        ├── TestHealthCheck  # 헬스 체크 (1개)
        ├── TestJobEndpoints # Job API (7개)
        ├── TestTaskEndpoints # Task API (2개)
        ├── TestDocumentEndpoints # Document API (4개)
        ├── TestStatisticsEndpoints (1개)
        ├── TestRateLimiting # Rate Limiting (1개)
        └── TestErrorHandling # 에러 처리 (3개)
```

### 8.2 테스트 커버리지

**현재 커버리지**: 19%

| 모듈 | 라인 수 | 커버 | 비율 |
|------|---------|------|------|
| config.py | 145 | 93 | 64% |
| crud.py | 164 | 112 | 68% |
| models.py | 101 | 101 | **100%** |
| database.py | 38 | 17 | 45% |
| tasks.py | 246 | 0 | 0% |
| college_crawlers.py | 242 | 0 | 0% |

**목표 커버리지**: 80%

**우선순위**:
1. ✅ models.py (100%) - 완료
2. ✅ crud.py (68%) - 완료
3. ✅ config.py (64%) - 완료
4. ⏳ tasks.py (0% → 70%) - 다음 목표
5. ⏳ college_crawlers.py (0% → 60%) - 다음 목표

### 8.3 테스트 실행

```bash
# 전체 테스트
PYTHONPATH=. pytest

# 커버리지 포함
PYTHONPATH=. pytest --cov=. --cov-report=html

# 특정 테스트 파일
PYTHONPATH=. pytest tests/unit/test_crud.py -v

# 특정 테스트 클래스
PYTHONPATH=. pytest tests/unit/test_crud.py::TestJobCRUD -v

# 마커 기반
PYTHONPATH=. pytest -m "unit" -v
```

### 8.4 테스트 예시

#### 단위 테스트 (test_crud.py)
```python
def test_create_job(db_session):
    """잡 생성 테스트"""
    job_data = {
        "name": "test-job",
        "priority": "P1",
        "seed_type": "URL_LIST",
        "seed_payload": {"urls": ["https://example.com"]},
        "render_mode": "STATIC",
        "robots_policy": "OBEY",
    }

    job = crud.create_job(db_session, job_data)

    assert job.id is not None
    assert job.name == job_data["name"]
    assert job.status == JobStatus.ACTIVE
```

#### 통합 테스트 (test_api_endpoints.py)
```python
def test_create_job_api(client, api_key):
    """Job 생성 API 테스트"""
    response = client.post(
        "/jobs",
        json={
            "name": "api-test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]}
        },
        headers={"X-API-Key": api_key}
    )

    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "api-test-job"
    assert "id" in data
```

---

## 9. 배포 및 운영

### 9.1 Docker Compose 구성

```yaml
version: '3.8'

services:
  # FastAPI 서버
  app:
    build: ./app
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - CELERY_BROKER_URL=${CELERY_BROKER_URL}
      - API_KEY=${API_KEY}
    depends_on:
      - postgres
      - redis
    volumes:
      - ./app:/app

  # Celery Worker
  celery_worker:
    build: ./app
    command: celery -A worker_app worker --loglevel=info
    environment:
      - CELERY_BROKER_URL=${CELERY_BROKER_URL}
    depends_on:
      - redis
      - postgres

  # Celery Beat (스케줄러)
  celery_beat:
    build: ./app
    command: celery -A worker_app beat --loglevel=info
    environment:
      - CELERY_BROKER_URL=${CELERY_BROKER_URL}
    depends_on:
      - redis

  # PostgreSQL
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_USER=crawler
      - POSTGRES_PASSWORD=${DB_PASSWORD}
      - POSTGRES_DB=school_notices
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  # Redis
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Prometheus
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  # Grafana
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  postgres_data:
  grafana_data:
```

### 9.2 배포 체크리스트

#### 사전 준비
- [ ] `.env` 파일 생성 및 모든 필수 환경 변수 설정
- [ ] `SECRET_KEY` 생성 (64자 이상)
- [ ] `API_KEY` 생성 (32자 이상)
- [ ] 데이터베이스 비밀번호 변경
- [ ] `ENV=production` 설정
- [ ] `DEBUG=false` 확인

#### 보안
- [ ] HTTPS 설정 (Let's Encrypt)
- [ ] `ALLOWED_ORIGINS` 실제 도메인으로 제한
- [ ] `TRUSTED_HOSTS` 설정
- [ ] Rate Limiting 활성화
- [ ] Firewall 설정 (포트 제한)

#### 모니터링
- [ ] Sentry DSN 설정
- [ ] Prometheus 메트릭 확인
- [ ] Grafana 대시보드 구성
- [ ] 로그 수집 설정 (ELK Stack 또는 CloudWatch)

#### 백업
- [ ] PostgreSQL 자동 백업 설정
- [ ] 백업 복원 테스트
- [ ] Redis 스냅샷 설정

### 9.3 배포 명령어

```bash
# 1. 환경 변수 설정
cp app/.env.example app/.env
nano app/.env  # 환경 변수 편집

# 2. Docker 빌드 및 실행
docker-compose build
docker-compose up -d

# 3. 데이터베이스 마이그레이션
docker-compose exec app alembic upgrade head

# 4. 헬스 체크
curl http://localhost:8000/health

# 5. 로그 확인
docker-compose logs -f app
docker-compose logs -f celery_worker

# 6. 서비스 재시작
docker-compose restart app

# 7. 스케일링 (Worker 증설)
docker-compose up -d --scale celery_worker=3
```

### 9.4 헬스 체크

**엔드포인트**: `GET /health`

**응답 예시**:
```json
{
  "status": "healthy",
  "timestamp": "2025-11-03T12:00:00Z",
  "database": "connected",
  "redis": "connected",
  "celery": {
    "workers": 2,
    "active_tasks": 5
  }
}
```

### 9.5 모니터링 대시보드

#### Grafana 주요 패널
1. **HTTP Requests** (QPS)
2. **Response Time** (P50, P95, P99)
3. **Error Rate** (4xx, 5xx)
4. **Active Crawl Tasks**
5. **Crawl Success Rate**
6. **Database Connection Pool**
7. **Redis Memory Usage**
8. **Celery Queue Length**

---

## 10. 개발 가이드

### 10.1 새로운 크롤러 추가

#### Step 1: 크롤러 클래스 작성

```python
# college_crawlers.py

class NewUniversityCrawler(BaseCrawler):
    """새 대학교 크롤러"""

    BASE_URL = "https://www.newuniv.ac.kr"
    NOTICE_URL = f"{BASE_URL}/notice"

    async def crawl(self, url: str) -> List[Dict]:
        """크롤링 실행"""

        # HTML 가져오기 (동적 페이지면 use_browser=True)
        html = await self.fetch_html(url, use_browser=False)

        # 파싱
        soup = BeautifulSoup(html, 'lxml')

        notices = []
        for item in soup.select('.notice-item'):  # 셀렉터 수정 필요
            notice = {
                "title": item.select_one('.title').text.strip(),
                "url": urljoin(self.BASE_URL, item.select_one('a')['href']),
                "date": item.select_one('.date').text.strip(),
                "category": self._extract_category(item),
                "source": "newuniv",
                "fingerprint": self._generate_fingerprint(item['href'])
            }
            notices.append(notice)

        return notices

    def _extract_category(self, item) -> str:
        """카테고리 추출 (대학마다 다름)"""
        # 구현 필요
        pass
```

#### Step 2: 크롤러 등록

```python
# college_crawlers.py 하단

COLLEGE_CRAWLERS = {
    "konkuk": KonkukCrawler,
    "seoultech": SeoulTechCrawler,
    "newuniv": NewUniversityCrawler,  # 추가
}
```

#### Step 3: 테스트 작성

```python
# tests/unit/test_crawlers.py

async def test_newuniv_crawler():
    """새 대학 크롤러 테스트"""
    crawler = NewUniversityCrawler()

    results = await crawler.crawl("https://www.newuniv.ac.kr/notice")

    assert len(results) > 0
    assert all("title" in r for r in results)
    assert all("url" in r for r in results)
```

### 10.2 새로운 API 엔드포인트 추가

#### Step 1: Pydantic 스키마 정의

```python
# schemas.py

class CustomReportRequest(BaseModel):
    """커스텀 리포트 요청"""
    start_date: datetime
    end_date: datetime
    colleges: List[str]

class CustomReportResponse(BaseModel):
    """커스텀 리포트 응답"""
    total_notices: int
    by_college: Dict[str, int]
    by_category: Dict[str, int]
```

#### Step 2: CRUD 함수 추가

```python
# crud.py

def generate_custom_report(
    db: Session,
    start_date: datetime,
    end_date: datetime,
    colleges: List[str]
) -> Dict[str, Any]:
    """커스텀 리포트 생성"""

    query = db.query(CrawlNotice).filter(
        CrawlNotice.created_at >= start_date,
        CrawlNotice.created_at <= end_date,
        CrawlNotice.source.in_(colleges)
    )

    notices = query.all()

    # 집계
    by_college = {}
    by_category = {}

    for notice in notices:
        by_college[notice.source] = by_college.get(notice.source, 0) + 1
        by_category[notice.category] = by_category.get(notice.category, 0) + 1

    return {
        "total_notices": len(notices),
        "by_college": by_college,
        "by_category": by_category
    }
```

#### Step 3: API 라우터 추가

```python
# api.py

@router.post("/reports/custom", response_model=CustomReportResponse)
async def generate_report(
    request: CustomReportRequest,
    db: Session = Depends(get_db),
    api_key: str = Depends(verify_api_key)
):
    """커스텀 리포트 생성"""

    report = crud.generate_custom_report(
        db,
        request.start_date,
        request.end_date,
        request.colleges
    )

    return report
```

### 10.3 코드 스타일 가이드

#### Python 스타일
```bash
# 코드 포맷팅
black app/ --line-length 100

# Import 정렬
isort app/

# 린팅
flake8 app/ --max-line-length 100

# 타입 체크
mypy app/ --ignore-missing-imports
```

#### Commit 메시지
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
refactor: 리팩토링
test: 테스트 추가
chore: 빌드/설정 변경
```

### 10.4 트러블슈팅

#### 문제 1: PostgreSQL 연결 오류
```bash
# 증상
sqlalchemy.exc.OperationalError: could not connect to server

# 해결
1. PostgreSQL 서비스 확인
   docker-compose ps postgres

2. 연결 정보 확인
   echo $DATABASE_URL

3. 네트워크 확인
   docker-compose exec app ping postgres

4. 재시작
   docker-compose restart postgres
```

#### 문제 2: Celery 작업 실행 안됨
```bash
# 증상
작업이 Redis 큐에만 쌓이고 실행 안됨

# 해결
1. Worker 상태 확인
   docker-compose ps celery_worker

2. Worker 로그 확인
   docker-compose logs celery_worker

3. Redis 연결 확인
   docker-compose exec redis redis-cli ping

4. Worker 재시작
   docker-compose restart celery_worker
```

#### 문제 3: Rate Limit 429 오류
```bash
# 증상
{"error": "Rate limit exceeded"}

# 해결
1. Redis에서 Rate Limit 키 확인
   docker-compose exec redis redis-cli KEYS "rate_limit:*"

2. 특정 클라이언트 리셋
   docker-compose exec redis redis-cli DEL "rate_limit:api_key:YOUR_KEY"

3. 설정 조정 (.env)
   MAX_REQUESTS_PER_MINUTE=120
```

---

## 부록

### A. API 엔드포인트 전체 목록

| 엔드포인트 | 메서드 | 설명 | 인증 | 페이징 |
|-----------|--------|------|------|--------|
| `/health` | GET | 헬스 체크 | ❌ | ❌ |
| `/metrics` | GET | Prometheus 메트릭 | ❌ | ❌ |
| `/jobs` | GET | Job 목록 조회 | ✅ | ✅ |
| `/jobs` | POST | Job 생성 | ✅ | ❌ |
| `/jobs/{id}` | GET | Job 조회 | ✅ | ❌ |
| `/jobs/{id}` | DELETE | Job 삭제 | ✅ | ❌ |
| `/jobs/{id}/pause` | POST | Job 일시정지 | ✅ | ❌ |
| `/jobs/{id}/resume` | POST | Job 재개 | ✅ | ❌ |
| `/jobs/{id}/tasks` | GET | Job의 Task 목록 | ✅ | ✅ |
| `/jobs/{id}/statistics` | GET | Job 통계 | ✅ | ❌ |
| `/tasks` | GET | Task 목록 | ✅ | ✅ |
| `/tasks/{id}` | GET | Task 조회 | ✅ | ❌ |
| `/documents` | GET | 공지사항 목록 | ✅ | ✅ |
| `/documents/{id}` | GET | 공지사항 조회 | ✅ | ❌ |
| `/documents/search` | GET | 공지사항 검색 | ✅ | ✅ |
| `/run-crawler/{name}` | POST | 크롤러 수동 실행 | ✅ | ❌ |

### B. 데이터베이스 스키마

```sql
-- crawl_job 테이블
CREATE TABLE crawl_job (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    priority VARCHAR(4) NOT NULL,
    schedule_cron VARCHAR(64),
    seed_type VARCHAR(20) NOT NULL,
    seed_payload JSONB NOT NULL,
    render_mode VARCHAR(20) NOT NULL,
    rate_limit_per_host FLOAT DEFAULT 1.0,
    max_depth INTEGER DEFAULT 1,
    robots_policy VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_job_name ON crawl_job(name);
CREATE INDEX idx_job_status ON crawl_job(status);

-- crawl_task 테이블
CREATE TABLE crawl_task (
    id SERIAL PRIMARY KEY,
    job_id INTEGER NOT NULL REFERENCES crawl_job(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    retries INTEGER DEFAULT 0,
    last_error TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    http_status INTEGER,
    content_hash VARCHAR(64),
    blocked_flag BOOLEAN DEFAULT FALSE,
    cost_ms_browser INTEGER
);

CREATE INDEX idx_task_job_id ON crawl_task(job_id);
CREATE INDEX idx_task_status ON crawl_task(status);
CREATE INDEX idx_task_url ON crawl_task(url);

-- crawl_notice 테이블
CREATE TABLE crawl_notice (
    id SERIAL PRIMARY KEY,
    job_id INTEGER NOT NULL REFERENCES crawl_job(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    title TEXT,
    writer VARCHAR(128),
    date VARCHAR(32),
    category VARCHAR(64),
    source VARCHAR(64),
    extracted JSONB,
    raw TEXT,
    fingerprint VARCHAR(64) NOT NULL UNIQUE,
    snapshot_version VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_notice_job_id ON crawl_notice(job_id);
CREATE INDEX idx_notice_url ON crawl_notice(url);
CREATE INDEX idx_notice_category ON crawl_notice(category);
CREATE INDEX idx_notice_fingerprint ON crawl_notice(fingerprint);
CREATE INDEX idx_notice_created_at ON crawl_notice(created_at);
```

### C. Celery Beat 스케줄 예시

```python
from celery.schedules import crontab

celery_app.conf.beat_schedule = {
    # 매일 오전 6시 전체 크롤링
    'daily-morning-crawl': {
        'task': 'tasks.crawl_all_universities',
        'schedule': crontab(hour=6, minute=0),
    },

    # 6시간마다 장학금 공지 크롤링
    'scholarship-crawl': {
        'task': 'tasks.crawl_scholarship_notices',
        'schedule': crontab(hour='*/6'),
    },

    # 매시간 헬스 체크
    'hourly-health-check': {
        'task': 'tasks.health_check',
        'schedule': crontab(minute=0),
    },

    # 매일 자정 통계 집계
    'daily-statistics': {
        'task': 'tasks.generate_daily_statistics',
        'schedule': crontab(hour=0, minute=0),
    }
}
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2025-11-03 | 최초 작성 |

---

**문의**: [GitHub Issues](https://github.com/your-repo/issues)
