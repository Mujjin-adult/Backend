# 크롤링 서버 아키텍처 및 작동 원리

## 📐 전체 시스템 아키텍처

```
┌────────────────────────────────────────────────────────────────┐
│                    크롤링 서버 (FastAPI)                        │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  FastAPI     │  │  Celery      │  │  Auto        │        │
│  │  Web Server  │  │  Worker      │  │  Scheduler   │        │
│  │  (main.py)   │  │  (tasks.py)  │  │  (auto_      │        │
│  │              │  │              │  │  scheduler)  │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
│         │                  │                  │                 │
│         │                  │                  │                 │
│         ▼                  ▼                  ▼                 │
│  ┌──────────────────────────────────────────────────────┐     │
│  │             College Crawler (college_crawlers.py)     │     │
│  │  - crawl_volunteer()                                  │     │
│  │  - crawl_job()                                        │     │
│  │  - crawl_scholarship()                                │     │
│  │  - crawl_all()                                        │     │
│  └──────────────────────────────────────────────────────┘     │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Circuit     │  │  Rate        │  │  Robots.txt  │        │
│  │  Breaker     │  │  Limiter     │  │  Parser      │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└────────────────────────────────────────────────────────────────┘
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                   외부 시스템                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ PostgreSQL   │  │ Redis        │  │ 인천대 웹사이트│         │
│  │ (crawl_      │  │ (Celery      │  │ (www.inu.    │         │
│  │ notice,      │  │ Broker/      │  │ ac.kr)       │         │
│  │ crawl_job)   │  │ Result)      │  │              │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 크롤링 서버 작동 흐름

### 1️⃣ 시스템 시작 (Startup)

**main.py의 lifespan 함수**

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. 시작 시 초기화
    print("Starting College Notice Crawler...")

    # 2. 데이터베이스 초기화
    init_database()  # 테이블 생성, 연결 확인

    # 3. 스케줄러 초기화
    init_college_scheduler()
        ↓
        ├─ CollegeAutoScheduler 인스턴스 생성
        ├─ register_all_jobs() - 크롤링 작업을 DB에 등록
        └─ update_celery_schedule() - Celery Beat 스케줄 업데이트

    yield  # 애플리케이션 실행

    # 종료 시
    print("Shutting down...")
```

**초기화 과정:**
1. FastAPI 서버 시작
2. 데이터베이스 연결 및 테이블 생성
3. 8개 크롤링 작업을 `crawl_job` 테이블에 등록
4. Celery Beat에 스케줄 등록

---

### 2️⃣ 자동 스케줄러 (Auto Scheduler)

**auto_scheduler.py의 CollegeAutoScheduler 클래스**

#### 작업 등록 (register_all_jobs)

```python
job_configs = [
    {
        "name": "봉사 공지사항 크롤링",
        "priority": "P1",
        "seed_payload": {
            "urls": ["https://www.inu.ac.kr/bbs/inu/253/artclList.do"],
            "category": "volunteer",
            "page_num": "253"
        },
        "schedule_cron": "0 */2 * * *",  # 2시간마다
        "rate_limit_per_host": 0.5,      # 2초에 1회
        "max_depth": 1
    },
    # ... 7개 작업 더 (취업, 장학금, 일반행사, 교육시험, 등록금납부, 학점, 학위)
]
```

**등록 과정:**
```
1. job_configs 리스트 순회
   ↓
2. 각 작업을 DB에 등록 (중복 체크)
   ↓
3. crawl_job 테이블에 저장
   - name: "봉사 공지사항 크롤링"
   - status: ACTIVE
   - schedule_cron: "0 */2 * * *"
   - seed_payload: JSON 형태로 저장
```

#### 스케줄 업데이트 (update_celery_schedule)

```python
for config in job_configs:
    schedule_name = "college-봉사-공지사항-크롤링"

    # crontab 문자열 파싱: "0 */2 * * *"
    # → minute=0, hour=*/2, day=*, month=*, day_of_week=*

    celery_app.conf.beat_schedule[schedule_name] = {
        "task": "tasks.college_crawl_task",  # 실행할 태스크
        "schedule": crontab(minute=0, hour="*/2"),  # 2시간마다
        "args": ("봉사 공지사항 크롤링",),  # job_name
        "options": {"priority": 1}  # P1 = 우선순위 1
    }
```

**스케줄 등록 결과:**
```
Celery Beat 스케줄:
├─ college-봉사-공지사항-크롤링    → 2시간마다 실행 (0 */2 * * *)
├─ college-취업-공지사항-크롤링    → 3시간마다 실행 (0 */3 * * *)
├─ college-장학금-공지사항-크롤링  → 4시간마다 실행 (0 */4 * * *)
├─ college-일반행사/채용-크롤링    → 6시간마다 실행 (0 */6 * * *)
├─ college-교육시험-크롤링         → 6시간마다 실행 (0 */6 * * *)
├─ college-등록금납부-크롤링       → 8시간마다 실행 (0 */8 * * *)
├─ college-학점-크롤링             → 8시간마다 실행 (0 */8 * * *)
└─ college-학위-크롤링             → 8시간마다 실행 (0 */8 * * *)
```

---

### 3️⃣ Celery 작업 큐 시스템

#### Celery 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Celery Beat                          │
│  (스케줄러 - 정해진 시간에 태스크 트리거)                │
│                                                          │
│  매 2시간마다:                                           │
│  college_crawl_task.delay("봉사 공지사항 크롤링")       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  Redis (Broker)                         │
│  - 태스크 큐 저장                                        │
│  - 메시지 브로커 역할                                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Celery Worker (tasks.py)                   │
│  - 큐에서 태스크 가져오기                                │
│  - college_crawl_task 실행                              │
│  - 결과를 Redis에 저장                                   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│          Redis (Result Backend)                         │
│  - 작업 결과 저장                                        │
│  - 성공/실패 상태                                        │
└─────────────────────────────────────────────────────────┘
```

#### Celery 설정 (tasks.py)

```python
celery_app = Celery(
    "school_notices",
    broker=REDIS_URL,           # Redis (메시지 큐)
    backend=REDIS_URL,          # Redis (결과 저장)
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="Asia/Seoul",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=30 * 60,        # 30분 최대 실행 시간
    task_soft_time_limit=25 * 60,   # 25분 소프트 타임아웃
    worker_prefetch_multiplier=1,   # 한 번에 1개 태스크만 가져옴
    worker_max_tasks_per_child=1000 # 1000개 작업 후 워커 재시작
)
```

---

### 4️⃣ 크롤링 태스크 실행 흐름

#### college_crawl_task (tasks.py)

**단계별 실행 흐름:**

```python
@celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
def college_crawl_task(self, job_name: str):
    """대학 공지사항 크롤링 태스크"""

    # 1단계: 초기화
    request_id = uuid.uuid4()
    start_time = time.time()

    # 2단계: 크롤링 실행 (job_name에 따라 분기)
    if job_name == "봉사 공지사항 크롤링":
        results = college_crawler.crawl_volunteer()
    elif job_name == "취업 공지사항 크롤링":
        results = college_crawler.crawl_job()
    elif job_name == "장학금 공지사항 크롤링":
        results = college_crawler.crawl_scholarship()
    else:
        results = college_crawler.crawl_all()

    # 3단계: 데이터베이스 저장
    #   ├─ 중복 URL 체크 (벌크 조회)
    #   ├─ 새 문서만 필터링
    #   ├─ fingerprint 생성 (SHA-256)
    #   └─ 벌크 삽입 (bulk_create_documents)

    # 4단계: 성능 로깅
    duration = time.time() - start_time
    log_performance(
        "college_crawl_task",
        duration,
        {"job_name": job_name, "total": 50, "saved": 10}
    )

    # 5단계: 결과 반환
    return {
        "status": "success",
        "job_name": job_name,
        "total_items": 50,
        "saved_items": 10,
        "skipped_items": 40,
        "duration": 15.2
    }
```

**재시도 로직:**

```python
# 예외 발생 시
except Exception as exc:
    # 지수 백오프로 재시도
    # 1차: 7초 후 (2^0 + 5 + random)
    # 2차: 9초 후 (2^1 + 5 + random)
    # 3차: 13초 후 (2^2 + 5 + random)
    self.retry(exc=exc, countdown=(2**self.request.retries) + 5)
```

---

### 5️⃣ 실제 크롤링 실행 (College Crawler)

#### CollegeCrawler 클래스 (college_crawlers.py)

**주요 기능:**
1. HTTP 요청 (Circuit Breaker, Rate Limiter 적용)
2. HTML 파싱 (BeautifulSoup)
3. 데이터 추출
4. 에러 처리

#### crawl_volunteer() 메서드 예시

```python
def crawl_volunteer(self) -> List[Dict[str, Any]]:
    """봉사 공지사항 크롤링"""

    url = "https://www.inu.ac.kr/bbs/inu/253/artclList.do"
    page_num = "253"
    source = "volunteer"
    all_notices = []

    # 최대 5페이지까지 크롤링
    for page in range(1, self.max_pages + 1):
        try:
            # 1단계: HTTP 요청 (재시도 로직 포함)
            payload = {
                "page": str(page),
                "srchBbsNttCd": page_num
            }
            response = self._make_request_with_retry(url, payload)

            # 2단계: HTML 파싱
            soup = BeautifulSoup(response.text, 'html.parser')
            rows = soup.select('table.board-table tbody tr')

            # 3단계: 각 행 파싱
            for row in rows:
                try:
                    notice = self._parse_notice_row(row, source)
                    if notice:
                        all_notices.append(notice)
                except Exception as e:
                    logger.error(f"Failed to parse row: {e}")
                    continue

            # 4단계: 페이지 간 딜레이 (Rate Limiting)
            time.sleep(random.uniform(1, 3))

        except TemporaryError as e:
            # 일시적 에러: 로깅만 하고 다음 페이지로
            logger.warning(f"Temporary error on page {page}: {e}")
            break

        except PermanentError as e:
            # 영구적 에러: 즉시 중단
            logger.error(f"Permanent error on page {page}: {e}")
            break

    return all_notices
```

#### _make_request_with_retry 메서드

**Circuit Breaker 패턴 적용:**

```python
def _make_request_with_retry(self, url: str, payload: dict, retry_count: int = 0):
    try:
        # Circuit Breaker로 보호된 요청
        def make_request():
            response = self.session.post(url, data=payload, timeout=30)

            # HTTP 상태 코드 분류
            if response.status_code == 429:
                raise TemporaryError("Rate limited")
            elif response.status_code in [500, 502, 503, 504]:
                raise TemporaryError("Server error")
            elif response.status_code in [400, 401, 403, 404]:
                raise PermanentError("Client error")

            return response

        # Circuit Breaker 실행
        return self.circuit_breaker.call(make_request)

    except TemporaryError as e:
        # 재시도 (최대 3회)
        if retry_count < self.max_retries:
            wait_time = (2 ** retry_count) + random.uniform(1, 3)
            time.sleep(wait_time)
            return self._make_request_with_retry(url, payload, retry_count + 1)
        else:
            raise
```

**Circuit Breaker 상태:**
```
CLOSED (정상)
   ↓ (5번 실패)
OPEN (차단)
   ↓ (60초 대기)
HALF_OPEN (테스트)
   ↓ (2번 성공)
CLOSED (복구)
```

---

### 6️⃣ 데이터 저장 흐름

#### 벌크 삽입 최적화

**Before (개별 삽입):**
```python
for item in results:
    create_document(db, item)  # 50개 → 50번 DB 쿼리
```

**After (벌크 삽입):**
```python
# 1단계: 중복 URL 체크 (1번 쿼리)
urls_to_check = [item['url'] for item in results]
existing_docs = db.query(CrawlNotice).filter(
    CrawlNotice.url.in_(urls_to_check)
).all()
existing_urls = {doc.url for doc in existing_docs}

# 2단계: 새 문서만 필터링
docs_to_insert = []
for item in results:
    if item['url'] not in existing_urls:
        docs_to_insert.append({
            "url": item['url'],
            "title": item['title'],
            "fingerprint": hashlib.sha256(...).hexdigest(),
            ...
        })

# 3단계: 벌크 삽입 (1번 쿼리)
db.bulk_insert_mappings(CrawlNotice, docs_to_insert)
db.commit()
```

**성능 개선:**
- 50개 문서 삽입: 50번 쿼리 → 2번 쿼리 (25배 개선)

---

### 7️⃣ 안정성 보장 메커니즘

#### Circuit Breaker

**목적:** 연속된 실패 시 시스템 보호

```python
circuit_breaker = CircuitBreaker(
    name="inu_crawler",
    failure_threshold=5,      # 5번 실패 시 차단
    success_threshold=2,      # 2번 성공 시 복구
    timeout=60.0              # 60초 대기
)

# CLOSED → OPEN → HALF_OPEN → CLOSED
```

#### Rate Limiter

**목적:** 서버 부하 방지

```python
rate_limiter = RateLimiter(
    requests_per_minute=60  # 분당 최대 60회 요청
)

# 사용
if not rate_limiter.can_make_request(url):
    rate_limiter.wait_for_request(url)
```

#### Robots.txt 준수

```python
robots_manager = RobotsManager()

# 크롤링 허용 확인
if robots_manager.is_allowed(url, user_agent):
    # crawl-delay 준수
    robots_manager.wait_if_needed(url, user_agent)
    # 크롤링 실행
```

---

## 🔄 전체 실행 흐름 (엔드 투 엔드)

### 자동 실행 (스케줄러)

```
1. Celery Beat 시작
   └─ 스케줄에 따라 태스크 트리거
      ├─ 00:00 - 봉사 크롤링 시작
      ├─ 02:00 - 봉사 크롤링 시작
      ├─ 03:00 - 취업 크롤링 시작
      ├─ 04:00 - 봉사, 장학금 크롤링 시작
      └─ ...

2. college_crawl_task.delay("봉사 공지사항 크롤링")
   └─ Redis 큐에 태스크 추가

3. Celery Worker
   └─ 큐에서 태스크 가져오기
      ├─ college_crawl_task 실행
      ├─ college_crawler.crawl_volunteer() 호출
      └─ 결과를 Redis에 저장

4. college_crawler.crawl_volunteer()
   └─ 실제 크롤링 수행
      ├─ HTTP 요청 (Circuit Breaker, Rate Limiter)
      ├─ HTML 파싱 (BeautifulSoup)
      ├─ 데이터 추출
      └─ 리스트 반환

5. 데이터베이스 저장
   └─ 중복 체크 → 벌크 삽입
      ├─ crawl_notice 테이블에 저장
      └─ 통계 업데이트

6. 완료
   └─ 성공/실패 로깅
      └─ 메트릭 수집 (Prometheus)
```

### 수동 실행 (API 호출)

```
1. POST /run-crawler/volunteer
   └─ API 키 인증

2. college_crawl_task.delay("봉사 공지사항 크롤링")
   └─ 백그라운드에서 실행

3. 응답 반환
   {
     "status": "success",
     "category": "volunteer",
     "message": "Crawling tasks triggered",
     "task_id": "abc-123-def"
   }

4. 백그라운드 실행 (위와 동일)
```

---

## 📊 모니터링 및 로깅

### Prometheus 메트릭

```python
# metrics.py
crawl_requests_total.inc()        # 총 요청 수
crawl_errors_total.inc()          # 에러 수
crawl_duration_seconds.observe()  # 실행 시간
circuit_breaker_state.set(1)      # Circuit Breaker 상태
```

### 로깅

```python
# logging_config.py
log_crawler_event("START", job_name, "PENDING")
log_crawler_event("COMPLETE", job_name, "SUCCESS")
log_performance("college_crawl_task", duration, {...})
log_error(exception, "crawl_volunteer", {...})
```

### Sentry 에러 추적

```python
# sentry_config.py
track_crawler_error(
    exception=e,
    source="volunteer",
    url=url,
    page=page
)
```

---

## 🎯 주요 설계 원칙

### 1. 안정성 (Reliability)
- **Circuit Breaker**: 연속 실패 시 시스템 보호
- **재시도 로직**: 지수 백오프로 일시적 에러 처리
- **Celery**: 비동기 작업 큐로 확장성 보장

### 2. 성능 (Performance)
- **벌크 삽입**: 데이터베이스 쿼리 최소화
- **중복 체크 최적화**: 한 번에 조회
- **Rate Limiting**: 서버 부하 방지

### 3. 유지보수성 (Maintainability)
- **모듈화**: 각 기능을 독립적인 모듈로 분리
- **로깅**: 모든 이벤트 추적
- **메트릭**: Prometheus로 성능 모니터링

### 4. 확장성 (Scalability)
- **Celery Worker**: 수평 확장 가능
- **Redis 큐**: 분산 처리 지원
- **Auto Scheduler**: 새 크롤링 작업 쉽게 추가

---

## 🚀 크롤링 작업 추가 방법

### 1. auto_scheduler.py에 작업 추가

```python
job_configs = [
    # 기존 작업들...
    {
        "name": "새로운 크롤링",
        "priority": "P2",
        "seed_payload": {
            "urls": ["https://www.inu.ac.kr/new/"],
            "category": "new_category",
            "page_num": "999"
        },
        "schedule_cron": "0 */3 * * *",  # 3시간마다
        "rate_limit_per_host": 0.5,
        "max_depth": 1
    }
]
```

### 2. college_crawlers.py에 크롤러 메서드 추가

```python
def crawl_new_category(self) -> List[Dict[str, Any]]:
    """새로운 카테고리 크롤링"""
    # 크롤링 로직 구현
    pass
```

### 3. tasks.py에 분기 추가

```python
if job_name == "새로운 크롤링":
    results = college_crawler.crawl_new_category()
```

### 4. 재시작

```bash
# 스케줄 강제 업데이트
POST /force-schedule-update

# 또는 서버 재시작
docker-compose restart crawling-server
```

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
