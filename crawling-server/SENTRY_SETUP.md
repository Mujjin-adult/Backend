# Sentry 에러 추적 설정 가이드

## 목차
- [Sentry란?](#sentry란)
- [Sentry 프로젝트 생성](#sentry-프로젝트-생성)
- [DSN 설정](#dsn-설정)
- [기능](#기능)
- [테스트](#테스트)
- [대시보드 활용](#대시보드-활용)

---

## Sentry란?

Sentry는 실시간 에러 추적 및 모니터링 플랫폼입니다.

### 주요 기능

- **실시간 에러 알림**: 에러 발생 즉시 알림
- **스택 트레이스**: 상세한 에러 발생 위치 및 콘텍스트
- **성능 모니터링**: API 응답 시간, 데이터베이스 쿼리 시간 추적
- **릴리스 추적**: 버전별 에러 추적
- **이슈 그룹화**: 유사한 에러 자동 그룹화

---

## Sentry 프로젝트 생성

### 1. Sentry 계정 생성

1. [Sentry.io](https://sentry.io) 접속
2. "Sign Up" 클릭
3. GitHub, Google 또는 이메일로 가입

### 2. 프로젝트 생성

1. 대시보드에서 "Create Project" 클릭
2. 플랫폼 선택: **Python**
3. 프로젝트 이름 입력: `college-notice-crawler`
4. 알림 빈도 설정 (기본값 권장)
5. "Create Project" 클릭

### 3. DSN 확인

프로젝트 생성 후 표시되는 DSN을 복사합니다.

형식: `https://<key>@<organization>.ingest.sentry.io/<project-id>`

---

## DSN 설정

### 1. 환경 변수 설정

`.env` 파일에 DSN을 추가합니다:

```bash
# Monitoring & Logging
SENTRY_DSN=https://your-key@your-org.ingest.sentry.io/your-project-id
ENABLE_PROMETHEUS=true
```

### 2. 서비스 재시작

```bash
docker-compose restart fastapi celery-worker
```

### 3. 초기화 확인

로그에서 Sentry 초기화 메시지 확인:

```bash
docker logs college_noti-fastapi-1 | grep -i sentry
```

출력 예시:
```
INFO: Sentry initialized successfully (environment: development)
```

---

## 기능

### 1. 자동 에러 추적

모든 예외가 자동으로 Sentry에 전송됩니다:

```python
# 크롤러 에러 자동 추적
try:
    response = crawler.crawl_volunteer()
except Exception as e:
    # 자동으로 Sentry에 전송됨
    logger.error(f"Crawler failed: {e}")
```

### 2. 크롤러 에러 추적

크롤러 에러는 추가 컨텍스트와 함께 추적됩니다:

```python
# college_crawlers.py에서 자동으로 호출됨
track_crawler_error(
    category="volunteer",
    error_type="TemporaryError",
    url="https://www.inu.ac.kr/...",
    exception=e,
    extra_data={"page": 1, "max_retries": 3}
)
```

**Sentry 대시보드에서 보이는 정보**:
- 에러 타입 (TemporaryError, PermanentError, etc.)
- 크롤링 카테고리 (volunteer, job, scholarship)
- URL
- 페이지 번호
- 재시도 횟수

### 3. 성능 모니터링

API 요청 및 데이터베이스 쿼리 성능 자동 추적:

- HTTP 요청 응답 시간
- 데이터베이스 쿼리 실행 시간
- Celery 작업 실행 시간
- Circuit Breaker 상태

### 4. 통합 모니터링

다음 서비스들의 에러가 자동으로 추적됩니다:

- **FastAPI**: API 엔드포인트 에러
- **SQLAlchemy**: 데이터베이스 쿼리 에러
- **Redis**: 캐시 작업 에러
- **Celery**: 백그라운드 작업 에러

---

## 테스트

### 1. 테스트 에러 발생

의도적으로 에러를 발생시켜 Sentry가 작동하는지 확인합니다:

```bash
# 존재하지 않는 카테고리로 크롤링 실행
curl -X POST 'http://localhost:8000/run-crawler/invalid_category' \
  -H 'X-API-Key: dev-api-key-12345'
```

### 2. Sentry 대시보드 확인

1. [Sentry.io](https://sentry.io) 로그인
2. `college-notice-crawler` 프로젝트 선택
3. "Issues" 탭에서 발생한 에러 확인

### 3. 에러 상세 정보 확인

에러 클릭 시 다음 정보를 확인할 수 있습니다:

- **Stack Trace**: 에러 발생 위치
- **Request**: HTTP 요청 정보 (메서드, URL, 헤더)
- **Tags**: 크롤러 카테고리, 에러 타입
- **Breadcrumbs**: 에러 발생 전 실행된 작업들
- **Environment**: 환경 (development, production)

---

## 대시보드 활용

### 1. 이슈 관리

**이슈 상태**:
- `Unresolved`: 해결되지 않은 에러
- `Resolved`: 해결된 에러
- `Ignored`: 무시할 에러

**이슈 할당**:
1. 이슈 클릭
2. "Assign to..." 클릭
3. 팀원 선택

### 2. 알림 설정

**Slack 알림**:
1. Settings → Integrations
2. Slack 선택 및 연결
3. 알림 조건 설정

**이메일 알림**:
1. Settings → Notifications
2. 알림 빈도 설정:
   - Immediately: 즉시
   - Hourly: 매시간
   - Daily: 매일

### 3. 릴리스 추적

새 버전 배포 시 릴리스 정보 전송:

```bash
# 릴리스 생성
sentry-cli releases new "college-notice-crawler@1.1.0"

# 배포 완료
sentry-cli releases finalize "college-notice-crawler@1.1.0"
```

### 4. 성능 대시보드

**Transaction 탭**:
- API 엔드포인트별 평균 응답 시간
- 가장 느린 엔드포인트 확인
- P50, P95, P99 응답 시간

**Database 탭**:
- 데이터베이스 쿼리 시간
- 느린 쿼리 확인
- N+1 문제 감지

### 5. 커스텀 대시보드

**주요 메트릭**:
1. Discover → Build New Query
2. 크롤러 에러율 쿼리:
   ```
   event.type:error AND tags[crawler.category]:volunteer
   ```
3. "Save as Dashboard" 클릭

---

## 모범 사례

### 1. 민감 정보 보호

`.env` 파일의 민감 정보는 자동으로 필터링됩니다:

```python
# sentry_config.py의 before_send_handler에서 자동 처리
headers = {
    "authorization": "[Filtered]",
    "x-api-key": "[Filtered]",
    "cookie": "[Filtered]"
}
```

### 2. 적절한 로그 레벨

```python
# ERROR 레벨 이상만 Sentry로 전송됨
logger.info("Crawling started")  # Sentry에 전송 안됨
logger.error("Crawling failed")  # Sentry에 전송됨
```

### 3. 컨텍스트 추가

에러 발생 시 유용한 정보 추가:

```python
from app.sentry_config import capture_exception_with_context

try:
    result = process_data(data)
except Exception as e:
    capture_exception_with_context(e, context={
        "data_size": len(data),
        "processing_step": "validation",
        "user_id": user_id
    })
```

### 4. 불필요한 에러 무시

예상되는 에러는 Sentry로 전송하지 않음:

```python
# 사용자 입력 검증 에러는 일반 로깅만
if not is_valid_input(data):
    logger.warning("Invalid input from user")
    raise HTTPException(status_code=400, detail="Invalid input")
```

---

## 문제 해결

### 1. Sentry가 초기화되지 않음

**증상**: 로그에 "Sentry DSN not configured" 메시지

**해결**:
1. `.env` 파일에 `SENTRY_DSN` 설정 확인
2. DSN 형식 확인 (https로 시작해야 함)
3. 서비스 재시작

### 2. 에러가 Sentry에 나타나지 않음

**확인 사항**:
1. Sentry 프로젝트가 활성화되어 있는지 확인
2. 네트워크 연결 확인 (방화벽 설정)
3. 로그 레벨 확인 (ERROR 이상만 전송됨)

**테스트**:
```python
from app.sentry_config import capture_message_with_level

capture_message_with_level("Test message", level="error")
```

### 3. 너무 많은 에러가 전송됨

**해결**:
1. `sample_rate` 조정 (sentry_config.py):
   ```python
   sample_rate=0.5  # 50%만 전송
   ```
2. 특정 에러 무시:
   ```python
   ignore_errors=[
       KeyboardInterrupt,
       "SpecificErrorClass",
   ]
   ```

---

## Sentry 프로젝트 구조

```
app/
├── sentry_config.py          # Sentry 초기화 및 설정
│   ├── init_sentry()         # Sentry SDK 초기화
│   ├── before_send_handler() # 민감 정보 필터링
│   ├── track_crawler_error() # 크롤러 에러 추적
│   └── capture_*()           # 수동 에러/메시지 캡처
│
├── main.py                    # Sentry 초기화 호출
├── college_crawlers.py        # 크롤러 에러 자동 추적
└── .env                       # SENTRY_DSN 설정
```

---

## 참고 자료

- [Sentry 공식 문서](https://docs.sentry.io/)
- [Python SDK 문서](https://docs.sentry.io/platforms/python/)
- [FastAPI 통합](https://docs.sentry.io/platforms/python/guides/fastapi/)
- [Celery 통합](https://docs.sentry.io/platforms/python/guides/celery/)

---

**작성일**: 2025-11-02
**버전**: 1.0
**작성자**: Claude Code Assistant
