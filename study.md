# 🏫 인천대학교 공지사항 앱 시스템 구조 학습 가이드

전체 시스템을 **메인 서버(Spring Boot)**와 **크롤링 서버(FastAPI)** 두 부분으로 나눠서 자세히 설명합니다.

---

## 📐 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                         사용자 (모바일 앱)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │ REST API
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    메인 서버 (Spring Boot)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ • 사용자 인증/인가 (JWT)                                    │  │
│  │ • 공지사항 조회 API                                         │  │
│  │ • 북마크/알림 관리                                          │  │
│  │ • Redis 캐싱                                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ PostgreSQL (통합 DB)
                         │ - crawl_notice 테이블
                         │ - users, categories, bookmarks 등
                         │
┌────────────────────────┴────────────────────────────────────────┐
│                   크롤링 서버 (FastAPI + Celery)                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ FastAPI (8001)      Celery Worker      Celery Beat       │  │
│  │ • API 엔드포인트    • 크롤링 작업 실행  • 스케줄러         │  │
│  │ • 크롤러 트리거     • 데이터 저장       • 자동 실행         │  │
│  │ • 대시보드         • 에러 처리         • (2~8시간마다)     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ▲                                   │
│                              │ Redis (메시지 큐)                  │
└──────────────────────────────┴──────────────────────────────────┘
                         │
                         ▼
              인천대학교 공지사항 웹사이트
              (https://www.inu.ac.kr)
```

---

## 🔷 1. 메인 서버 (Spring Boot) 구조

### 📦 기술 스택
- **Spring Boot 3.2.1** (Java 기반)
- **PostgreSQL** (메인 데이터베이스)
- **Redis** (캐싱 및 세션 관리)
- **JWT** (사용자 인증)
- **Swagger/OpenAPI** (API 문서화)

### 🗂️ 주요 디렉토리 구조
```
src/main/java/com/incheon/notice/
├── IncheonNoticeApplication.java    # 메인 애플리케이션
├── config/                          # 설정 파일들
│   ├── SecurityConfig.java          # Spring Security 설정
│   ├── SwaggerConfig.java           # API 문서 설정
│   ├── RedisConfig.java             # Redis 캐시 설정
│   └── JpaConfig.java               # JPA 설정
├── entity/                          # 데이터베이스 엔티티
│   ├── User.java                    # 사용자
│   ├── CrawlNotice.java            # 크롤링된 공지사항 ⭐
│   ├── Category.java                # 카테고리
│   ├── Bookmark.java                # 북마크
│   └── NotificationHistory.java    # 알림 히스토리
├── repository/                      # DB 접근 계층 (JPA)
│   ├── UserRepository.java
│   ├── CrawlNoticeRepository.java  # 크롤링 공지사항 조회
│   └── BookmarkRepository.java
├── service/                         # 비즈니스 로직
│   ├── AuthService.java            # 인증/회원가입
│   ├── NoticeService.java          # 공지사항 조회
│   └── CategoryService.java        # 카테고리 관리
├── controller/                      # REST API 엔드포인트
│   ├── AuthController.java         # 인증 API
│   ├── NoticeController.java       # 공지사항 API
│   └── CategoryController.java     # 카테고리 API
├── dto/                             # 데이터 전송 객체
│   ├── AuthDto.java
│   ├── NoticeDto.java
│   └── ApiResponse.java
└── security/                        # 보안 관련
    ├── JwtTokenProvider.java       # JWT 토큰 생성/검증
    └── JwtAuthenticationFilter.java # JWT 인증 필터
```

### 🎯 주요 기능

#### 1) **CrawlNotice 엔티티**
> 파일 위치: `src/main/java/com/incheon/notice/entity/CrawlNotice.java:29`

크롤러가 수집한 데이터를 메인 서버에서 읽어올 수 있도록 하는 핵심 엔티티입니다.

```java
@Entity
@Table(name = "crawl_notice")
public class CrawlNotice {
    // 크롤러 메타데이터
    private Long jobId;           // 크롤링 작업 ID
    private String url;           // 원본 URL
    private String fingerprint;   // 중복 체크용 해시

    // 공지사항 정보
    private String title;         // 제목
    private String content;       // 내용
    private String writer;        // 작성자
    private LocalDateTime publishedAt;  // 게시일

    // 사용자 기능
    private Boolean isImportant;  // 중요 공지 여부
    private Boolean isPinned;     // 상단 고정
    private List<Bookmark> bookmarks;  // 북마크 관계
}
```

#### 2) **NoticeController - 공지사항 조회 API**
> 파일 위치: `src/main/java/com/incheon/notice/controller/NoticeController.java:25`

공지사항 조회 API를 제공합니다.

**주요 엔드포인트:**
- `GET /api/notices` - 전체 공지사항 목록 (페이징)
- `GET /api/notices/{id}` - 공지사항 상세
- `GET /api/notices/search?keyword=장학금` - 검색
- `GET /api/notices/category/{code}` - 카테고리별 조회

```java
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getAllNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 공지사항 목록 조회 로직
    }
}
```

#### 3) **인증 시스템**
- **JWT 기반 토큰 인증**
- **Spring Security**로 엔드포인트 보호
- **Redis**에 리프레시 토큰 저장

---

## 🔶 2. 크롤링 서버 (FastAPI) 구조

### 📦 기술 스택
- **FastAPI 0.120.0** (Python 기반)
- **Celery 5.3.4** (비동기 작업 큐)
- **Redis** (Celery 브로커 및 결과 백엔드)
- **PostgreSQL** (메인 DB에 직접 저장)
- **BeautifulSoup4** (HTML 파싱)
- **SQLAlchemy** (ORM)

### 🗂️ 주요 디렉토리 구조
```
crawling-server/app/
├── main.py                    # FastAPI 애플리케이션 진입점
├── tasks.py                   # Celery 작업 정의
├── models.py                  # SQLAlchemy 모델 (DB 테이블)
├── college_crawlers.py        # 인천대 크롤러 로직
├── database.py                # DB 연결 설정
├── config.py                  # 환경 변수 설정
├── crud.py                    # DB CRUD 작업
├── api.py                     # API 라우터
├── auto_scheduler.py          # 자동 스케줄러
├── circuit_breaker.py         # Circuit Breaker 패턴
├── rate_limiter.py            # Rate Limiting
└── middleware/                # 미들웨어
    ├── security.py            # API 키 인증
    └── metrics_middleware.py  # Prometheus 메트릭
```

### 🎯 주요 구성 요소

#### 1) **FastAPI 서버**
> 파일 위치: `crawling-server/app/main.py:59`

크롤링을 트리거하고 데이터를 조회하는 REST API 서버

**주요 엔드포인트:**
- `GET /health` - 헬스 체크
- `POST /run-crawler/{category}` - 크롤링 실행 (API 키 필요)
- `GET /dashboard` - 크롤링 대시보드 (HTML)
- `GET /api/v1/documents/recent` - 최근 크롤링 문서
- `GET /api/v1/crawling-status` - 크롤링 작업 상태

**크롤링 카테고리:**
- `volunteer` - 봉사 공지사항
- `job` - 취업 공지사항
- `scholarship` - 장학금 공지사항
- `general_events` - 일반행사/채용
- `educational_test` - 교육시험
- `tuition_payment` - 등록금납부
- `academic_credit` - 학점
- `degree` - 학위
- `all` - 전체 크롤링

**크롤링 실행 예시:**
```bash
curl -X POST "http://localhost:8001/run-crawler/volunteer" \
  -H "X-API-Key: 0QWUQ6uNxMn4rvSqka4PeQx62ZtysZGF01VXBip0QjY"
```

#### 2) **Celery Worker - 크롤링 작업 실행**
> 파일 위치: `crawling-server/app/tasks.py:62`

백그라운드에서 실제 크롤링 작업을 수행하는 워커

```python
@celery_app.task(bind=True, max_retries=3)
def college_crawl_task(self, job_name: str):
    """
    대학 공지사항 크롤링 태스크

    작업 흐름:
    1. 인천대 웹사이트에서 HTML 크롤링
    2. BeautifulSoup로 데이터 파싱
    3. 중복 체크 (URL 기반)
    4. PostgreSQL에 저장 (bulk insert)
    5. 결과 반환
    """
```

**주요 기능:**
- **비동기 처리**: FastAPI 요청이 즉시 반환되고 백그라운드에서 처리
- **재시도 로직**: 실패 시 최대 3회 재시도
- **벌크 삽입**: 대량 데이터를 한 번에 삽입하여 성능 최적화
- **중복 방지**: URL 기반 중복 체크

#### 3) **Celery Beat - 자동 스케줄러**

주기적으로 크롤링을 자동 실행합니다.
- 각 카테고리마다 2~8시간마다 자동 실행
- `auto_scheduler.py`에서 스케줄 관리

#### 4) **CollegeCrawler - 크롤링 로직**
> 파일 위치: `crawling-server/app/college_crawlers.py:38`

인천대 웹사이트 크롤링 로직

```python
class CollegeCrawler:
    def __init__(self):
        self.base_url = "https://www.inu.ac.kr"
        self.circuit_breaker = get_circuit_breaker()  # 장애 대응

    def crawl_volunteer(self):
        """봉사 공지사항 크롤링"""
        # 1. HTTP 요청
        # 2. HTML 파싱
        # 3. 데이터 추출 (제목, 작성자, 날짜, URL 등)
        # 4. 결과 반환
```

**보호 메커니즘:**
- **Circuit Breaker**: 연속 실패 시 요청 중단 (서버 보호)
- **Rate Limiter**: 요청 속도 제한 (서버 부하 방지)
- **Retry Logic**: 실패 시 지수 백오프 재시도

#### 5) **데이터 모델**
> 파일 위치: `crawling-server/app/models.py:108`

```python
class CrawlNotice(Base):
    __tablename__ = "crawl_notice"

    # 크롤러 필드
    job_id: int                    # 크롤링 작업 ID
    url: str                       # 원본 URL
    fingerprint: str               # 중복 체크 해시

    # 공지사항 데이터
    title: str                     # 제목
    writer: str                    # 작성자
    date: str                      # 날짜
    category: str                  # 카테고리
    source: str                    # 소스 (volunteer, job 등)

    # 메인 서버 통합 필드
    external_id: str               # 외부 ID (중복 방지)
    category_id: int               # 카테고리 FK
    published_at: DateTime         # 게시일
    is_important: bool             # 중요 공지
```

---

## 🔄 3. 데이터 흐름

### 📥 크롤링 → 저장 흐름

```
1. 사용자 또는 스케줄러가 크롤링 트리거
   ↓
   POST /run-crawler/volunteer

2. FastAPI가 Celery 작업 큐에 태스크 등록
   ↓
   Redis 큐에 college_crawl_task 추가

3. Celery Worker가 태스크 가져와서 실행
   ↓
   college_crawl_task("봉사 공지사항 크롤링")

4. CollegeCrawler가 인천대 웹사이트 크롤링
   ↓
   • HTTP 요청: POST https://www.inu.ac.kr/...
   • HTML 파싱: BeautifulSoup
   • 데이터 추출: 제목, 작성자, 날짜, URL 등

5. 중복 체크 (URL 기반)
   ↓
   • 기존 URL이 DB에 있으면 스킵
   • 신규 데이터만 삽입 대상

6. PostgreSQL에 저장 (Bulk Insert)
   ↓
   • crawl_notice 테이블에 직접 저장
   • SQLAlchemy ORM 사용
   • 최대 50개씩 배치 삽입

7. 결과 반환
   ↓
   {
     "status": "success",
     "job_name": "봉사 공지사항 크롤링",
     "total_items": 50,
     "saved_items": 45,
     "skipped_items": 5,
     "duration": 12.5
   }
```

### 📤 사용자 조회 흐름

```
1. 사용자가 모바일 앱에서 공지사항 조회 요청
   ↓
   GET /api/notices?page=0&size=20

2. Spring Boot NoticeController가 요청 받음
   ↓
   NoticeController.getAllNotices()

3. NoticeService가 CrawlNoticeRepository 호출
   ↓
   noticeService.getAllNotices(pageable)

4. PostgreSQL에서 crawl_notice 테이블 조회
   ↓
   SELECT * FROM crawl_notice
   ORDER BY published_at DESC
   LIMIT 20 OFFSET 0

5. Redis 캐싱 (선택적)
   ↓
   • 자주 조회되는 데이터는 Redis에 캐시
   • TTL: 10분

6. DTO로 변환하여 JSON 반환
   ↓
   {
     "success": true,
     "message": "공지사항 목록 조회 성공",
     "data": {
       "content": [
         {
           "id": 1,
           "title": "2024학년도 장학금 신청 안내",
           "writer": "학생복지팀",
           "publishedAt": "2024-11-05T10:00:00",
           "category": "장학금",
           ...
         },
         ...
       ],
       "totalElements": 150,
       "totalPages": 8,
       "number": 0,
       "size": 20
     }
   }
```

---

## 🔗 4. 주요 통합 방식

### ✅ **DB 직접 연동** (현재 방식)
크롤링 서버가 메인 서버의 DB에 **직접 저장**합니다.

**장점:**
- ✅ 단순한 아키텍처
- ✅ 실시간 데이터 반영
- ✅ API 호출 없음 (네트워크 오버헤드 없음)
- ✅ 트랜잭션 관리 용이

**구성:**
```yaml
# crawling-server/.env
DATABASE_URL=postgresql://postgres:postgres@host.docker.internal:5432/incheon_notice
CELERY_BROKER_URL=redis://host.docker.internal:6379/0
CELERY_RESULT_BACKEND=redis://host.docker.internal:6379/0
```

크롤러가 `host.docker.internal`로 메인 서버의 PostgreSQL과 Redis에 접근합니다.

### 📊 통합 데이터베이스 테이블 구조

```
incheon_notice 데이터베이스
├── 크롤러 테이블 (FastAPI에서 관리)
│   ├── crawl_job           # 크롤링 작업 정의
│   ├── crawl_task          # 크롤링 태스크 실행 이력
│   ├── crawl_notice        # ⭐ 크롤링된 공지사항 (메인 테이블)
│   ├── host_budget         # 호스트별 크롤링 예산
│   └── webhook             # 웹훅 설정
│
└── 메인 서버 테이블 (Spring Boot에서 관리)
    ├── users               # 사용자
    ├── categories          # 카테고리
    ├── bookmarks           # 북마크 (crawl_notice_id FK)
    ├── notification_history # 알림 이력 (crawl_notice_id FK)
    └── user_preferences    # 사용자 설정
```

**FK 관계 변경:**
```sql
-- 기존 (deprecated)
bookmarks.notice_id → notices.id

-- 현재 (current)
bookmarks.crawl_notice_id → crawl_notice.id
```

---

## 🐳 5. Docker 구성

### 메인 서버 (docker-compose.yml)
```yaml
version: '3.8'

services:
  postgres:       # PostgreSQL (통합 DB)
    image: postgres:16-alpine
    ports: ["5432:5432"]

  redis:          # Redis (통합 캐시)
    image: redis:7-alpine
    ports: ["6379:6379"]

  backend:        # Spring Boot
    build: .
    ports: ["8080:8080"]
    depends_on:
      - postgres
      - redis

  pgadmin:        # DB 관리 도구
    image: dpage/pgadmin4
    ports: ["5050:80"]
```

### 크롤링 서버 (crawling-server/docker-compose.yml)
```yaml
version: '3.8'

services:
  fastapi:        # FastAPI 서버
    build: ./app
    ports: ["8001:8000"]

  celery-worker:  # Celery Worker
    build: ./app
    command: celery -A tasks worker --loglevel=INFO

  celery-beat:    # Celery Beat (스케줄러)
    build: ./app
    command: celery -A tasks beat --loglevel=INFO
```

### 🚀 실행 순서

**⚠️ 중요: 메인 서버를 먼저 시작해야 합니다** (크롤링 서버가 메인 DB/Redis 사용)

```bash
# 1단계: 메인 서버의 DB/Redis 먼저 시작
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app
docker-compose up -d postgres redis pgadmin

# 2단계: 크롤링 서버 시작
cd crawling-server
docker-compose up -d

# 3단계 (선택): Spring Boot 시작
cd ..
docker-compose up -d backend

# 서비스 확인
docker ps
```

### 서비스 중지
```bash
# 크롤링 서버 중지
cd crawling-server
docker-compose down

# 메인 서버 중지
cd ..
docker-compose down
```

---

## 🎓 6. 학습 포인트

### Spring Boot (메인 서버) - Java 백엔드
1. **레이어드 아키텍처**: Controller → Service → Repository 패턴
2. **JPA/Hibernate**: ORM으로 DB 접근, 엔티티 관계 매핑
3. **Spring Security**: JWT 기반 인증, 엔드포인트 보호
4. **Redis 캐싱**: 조회 성능 최적화
5. **Swagger/OpenAPI**: API 문서 자동 생성

### FastAPI (크롤링 서버) - Python 백엔드
1. **비동기 작업**: Celery로 백그라운드 처리
2. **크롤링 기술**: BeautifulSoup, HTTP 요청, HTML 파싱
3. **Circuit Breaker**: 장애 전파 방지, 시스템 안정성
4. **Rate Limiting**: 서버 부하 방지, 예의 바른 크롤링
5. **Bulk Insert**: 대량 데이터 삽입 최적화
6. **SQLAlchemy**: Python ORM

### 시스템 통합
1. **단일 DB 공유**: 두 서버가 같은 PostgreSQL 사용
2. **Docker Compose**: 컨테이너 오케스트레이션
3. **모니터링**: Prometheus + Grafana (선택적)
4. **데이터 동기화**: 실시간 DB 공유로 자동 동기화

---

## 📚 7. 주요 파일 위치 정리

### 메인 서버 (Spring Boot)
- **애플리케이션**: `src/main/java/com/incheon/notice/IncheonNoticeApplication.java`
- **공지사항 엔티티**: `src/main/java/com/incheon/notice/entity/CrawlNotice.java`
- **공지사항 API**: `src/main/java/com/incheon/notice/controller/NoticeController.java`
- **공지사항 서비스**: `src/main/java/com/incheon/notice/service/NoticeService.java`
- **설정 파일**: `src/main/resources/application.yml`
- **Docker 구성**: `docker-compose.yml`

### 크롤링 서버 (FastAPI)
- **FastAPI 앱**: `crawling-server/app/main.py`
- **Celery 태스크**: `crawling-server/app/tasks.py`
- **크롤러 로직**: `crawling-server/app/college_crawlers.py`
- **데이터 모델**: `crawling-server/app/models.py`
- **API 라우터**: `crawling-server/app/api.py`
- **설정 파일**: `crawling-server/.env`
- **Docker 구성**: `crawling-server/docker-compose.yml`

---

## 🔍 8. 트러블슈팅 가이드

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

### 크롤링 데이터 확인
```bash
# 데이터베이스 직접 쿼리
docker exec incheon-notice-db psql -U postgres -d incheon_notice \
  -c "SELECT COUNT(*) FROM crawl_notice;"

# 최근 크롤링된 공지사항 5개 조회
docker exec incheon-notice-db psql -U postgres -d incheon_notice \
  -c "SELECT id, title, source, created_at FROM crawl_notice ORDER BY created_at DESC LIMIT 5;"
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

---

## 📋 요약

### 메인 서버 (Spring Boot)
- **역할**: 사용자 API, 인증, 공지사항 조회, 북마크 관리
- **포트**: 8080
- **주요 기술**: Spring Boot, JPA, JWT, Redis
- **엔드포인트**: `/api/notices`, `/api/auth`, `/api/categories`

### 크롤링 서버 (FastAPI)
- **역할**: 인천대 공지사항 자동 수집, 데이터 저장
- **포트**: 8001
- **주요 기술**: FastAPI, Celery, BeautifulSoup, Circuit Breaker
- **엔드포인트**: `/run-crawler/{category}`, `/dashboard`, `/health`

### 통합 방식
- **데이터베이스**: 단일 PostgreSQL DB 공유 (`incheon_notice`)
- **데이터 흐름**: 크롤러가 `crawl_notice` 테이블에 직접 저장 → Spring Boot가 같은 테이블에서 조회
- **실시간 동기화**: DB 공유로 자동 동기화

---

## 🎯 학습 순서 추천

1. **Docker 환경 이해**: `docker-compose.yml` 파일 분석
2. **메인 서버 API 테스트**: Swagger UI로 엔드포인트 확인
3. **크롤링 서버 테스트**: 대시보드로 크롤링 실행
4. **데이터베이스 확인**: pgAdmin으로 테이블 구조 확인
5. **코드 분석**: Controller → Service → Repository 흐름 따라가기
6. **크롤러 분석**: FastAPI → Celery → CollegeCrawler 흐름 이해

---

**마지막 업데이트**: 2024-11-05
**프로젝트**: 인천대학교 공지사항 알림 앱
**개발 환경**: Docker, Spring Boot 3.2.1, FastAPI 0.120.0, PostgreSQL 16, Redis 7, Celery 5.3.4
