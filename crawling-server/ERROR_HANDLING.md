# 에러 처리 가이드

## 목차
- [Circuit Breaker 패턴](#circuit-breaker-패턴)
- [재시도 로직](#재시도-로직)
- [에러 분류](#에러-분류)
- [모니터링](#모니터링)
- [문제 해결](#문제-해결)

---

## Circuit Breaker 패턴

외부 서비스 호출 실패 시 시스템을 보호하고 복구를 위한 시간을 제공합니다.

### 작동 원리

Circuit Breaker는 3가지 상태를 가집니다:

```
┌────────┐  failures reach    ┌──────┐  timeout    ┌────────────┐
│ CLOSED │  threshold  ───────>│ OPEN │ ────────────>│ HALF_OPEN  │
└───┬────┘                     └──┬───┘              └─────┬──────┘
    │                             │                        │
    │ normal operation            │ fast fail              │ test recovery
    └─────────────────────────────┴────────────────────────┘
```

1. **CLOSED (정상 작동)**:
   - 모든 요청 허용
   - 실패 횟수 카운트
   - 실패 임계값 도달 시 OPEN으로 전환

2. **OPEN (차단 상태)**:
   - 모든 요청 즉시 거부 (빠른 실패)
   - 타임아웃 후 HALF_OPEN으로 전환
   - 시스템 복구 시간 제공

3. **HALF_OPEN (복구 시도)**:
   - 제한적 요청 허용
   - 성공 시 CLOSED로 전환
   - 실패 시 OPEN으로 되돌아감

### 설정

```python
from app.circuit_breaker import get_circuit_breaker

# Circuit Breaker 인스턴스 생성
breaker = get_circuit_breaker(
    name="external_api",
    failure_threshold=5,      # 5번 연속 실패 시 OPEN
    success_threshold=2,      # HALF_OPEN에서 2번 성공 시 CLOSED
    timeout=60.0,             # 60초 후 HALF_OPEN으로 전환
    expected_exception=TemporaryError
)

# 함수 호출 보호
try:
    result = breaker.call(external_api_function, arg1, arg2)
except CircuitBreakerError:
    # Circuit이 OPEN 상태
    logger.error("Circuit breaker is OPEN, service unavailable")
```

### 데코레이터 사용

```python
from app.circuit_breaker import circuit_breaker

@circuit_breaker(
    failure_threshold=3,
    timeout=30,
    name="external_service"
)
def call_external_service():
    return requests.get("https://api.example.com/data")
```

---

## 재시도 로직

### 지수 백오프 (Exponential Backoff)

실패 시 대기 시간을 지수적으로 증가시켜 재시도합니다.

```python
# 재시도 간격
# 1st retry: 2^0 + random(1,3) = 2~4초
# 2nd retry: 2^1 + random(1,3) = 3~5초
# 3rd retry: 2^2 + random(1,3) = 5~7초

wait_time = (2 ** retry_count) + random.uniform(1, 3)
```

### CollegeCrawler의 재시도

```python
crawler = CollegeCrawler(max_pages=5, max_retries=3)

# 자동으로 재시도 처리됨
results = crawler.crawl_volunteer()
```

### 재시도 가능 조건

- **재시도 가능** (Temporary Errors):
  - 429 Too Many Requests
  - 500, 502, 503, 504 Server Errors
  - Timeout
  - Connection Error

- **재시도 불가** (Permanent Errors):
  - 400 Bad Request
  - 401 Unauthorized
  - 403 Forbidden
  - 404 Not Found

---

## 에러 분류

### CrawlerHTTPError

크롤러 에러의 기본 클래스입니다.

```python
class CrawlerHTTPError(Exception):
    """크롤러 HTTP 에러 기본 클래스"""
    pass
```

### TemporaryError

일시적 에러로, 재시도가 가능합니다.

```python
class TemporaryError(CrawlerHTTPError):
    """일시적 에러 (재시도 가능)"""
    pass
```

**발생 상황**:
- 네트워크 일시 장애
- 서버 과부하 (429, 503)
- 타임아웃
- Circuit Breaker OPEN

**처리 방법**:
```python
try:
    response = crawler._make_request_with_retry(url, payload)
except TemporaryError as e:
    logger.warning(f"Temporary error, will retry: {e}")
    # 재시도 로직 또는 다음 페이지로 스킵
    continue
```

### PermanentError

영구적 에러로, 재시도하지 않습니다.

```python
class PermanentError(CrawlerHTTPError):
    """영구적 에러 (재시도 불가)"""
    pass
```

**발생 상황**:
- 잘못된 URL (404)
- 인증 실패 (401, 403)
- 잘못된 요청 (400)

**처리 방법**:
```python
try:
    response = crawler._make_request_with_retry(url, payload)
except PermanentError as e:
    logger.error(f"Permanent error, stopping: {e}")
    # 크롤링 중단
    break
```

---

## 모니터링

### Circuit Breaker 통계

```python
from app.circuit_breaker import get_all_circuit_breaker_stats

# 모든 Circuit Breaker 통계 조회
stats = get_all_circuit_breaker_stats()
print(stats)

# 출력 예시:
# {
#     "inu_crawler": {
#         "name": "inu_crawler",
#         "state": "CLOSED",
#         "failure_count": 0,
#         "success_count": 0,
#         "total_calls": 150,
#         "success_rate": 98.67,
#         "avg_duration": 0.245,
#         "last_failure_time": None
#     }
# }
```

### 로그 모니터링

에러 발생 시 다음과 같은 로그가 생성됩니다:

```
# Temporary Error (재시도)
2025-11-02 10:30:15 - WARNING - Temporary error, retrying in 3.2s (attempt 1/3): Server error (503): https://example.com

# Permanent Error (중단)
2025-11-02 10:30:20 - ERROR - Permanent error, not retrying: https://example.com

# Circuit Breaker
2025-11-02 10:30:25 - INFO - inu_crawler transitioning from CLOSED to OPEN (failures: 5/5)
2025-11-02 10:31:25 - INFO - inu_crawler transitioning from OPEN to HALF_OPEN
2025-11-02 10:31:30 - INFO - inu_crawler transitioning from HALF_OPEN to CLOSED
```

---

## 문제 해결

### Circuit Breaker가 OPEN 상태로 고정됨

**증상**: 크롤링이 계속 실패하고 "Circuit breaker is OPEN" 에러 발생

**원인**:
- 외부 서비스가 다운됨
- 네트워크 문제
- 실패 임계값이 너무 낮음

**해결 방법**:

1. **외부 서비스 상태 확인**:
   ```bash
   curl -I https://www.inu.ac.kr
   ```

2. **Circuit Breaker 수동 리셋**:
   ```python
   from app.circuit_breaker import get_circuit_breaker

   breaker = get_circuit_breaker("inu_crawler")
   breaker.reset()
   ```

3. **임계값 조정** (app/college_crawlers.py):
   ```python
   self.circuit_breaker = get_circuit_breaker(
       name="inu_crawler",
       failure_threshold=10,  # 5 → 10으로 증가
       timeout=120.0,         # 60 → 120으로 증가
   )
   ```

### 재시도가 너무 많음

**증상**: 같은 요청이 계속 재시도되어 시간이 오래 걸림

**원인**:
- `max_retries` 값이 너무 큼
- 영구적 에러를 일시적 에러로 잘못 분류

**해결 방법**:

1. **재시도 횟수 조정**:
   ```python
   crawler = CollegeCrawler(max_retries=2)  # 3 → 2로 감소
   ```

2. **에러 분류 확인**:
   - 400번대 에러는 PermanentError로 처리되어야 함
   - 500번대 에러만 TemporaryError로 처리

### 크롤링이 중간에 멈춤

**증상**: 일부 페이지만 크롤링되고 중단됨

**원인**:
- PermanentError 발생으로 크롤링 중단
- Circuit Breaker가 OPEN 상태

**해결 방법**:

1. **로그 확인**:
   ```bash
   docker logs college_noti-celery-worker-1 --tail 100 | grep ERROR
   ```

2. **에러 유형 확인**:
   - PermanentError면 URL이나 권한 문제
   - TemporaryError면 네트워크 또는 서버 문제

3. **재시작**:
   ```bash
   docker-compose restart celery-worker
   ```

### 타임아웃 에러가 자주 발생

**증상**: "Request timeout" 에러가 반복됨

**해결 방법**:

1. **타임아웃 값 증가** (app/college_crawlers.py):
   ```python
   response = self.session.post(url, data=payload, timeout=60)  # 30 → 60
   ```

2. **네트워크 확인**:
   ```bash
   ping www.inu.ac.kr
   traceroute www.inu.ac.kr
   ```

---

## 모범 사례

### 1. 적절한 에러 분류

```python
# ✅ 좋은 예시
if response.status_code == 404:
    raise PermanentError(f"Page not found: {url}")
elif response.status_code == 503:
    raise TemporaryError(f"Service unavailable: {url}")

# ❌ 나쁜 예시
raise Exception(f"Error: {response.status_code}")  # 분류 없음
```

### 2. Circuit Breaker 활용

```python
# ✅ 좋은 예시 - Circuit Breaker로 보호
breaker = get_circuit_breaker("external_api")
result = breaker.call(api_function)

# ❌ 나쁜 예시 - 보호 없음
result = api_function()  # 실패 시 시스템 전체 영향
```

### 3. 적절한 로깅

```python
# ✅ 좋은 예시
logger.warning(
    f"Temporary error, retrying in {wait_time:.1f}s "
    f"(attempt {retry_count + 1}/{self.max_retries}): {str(e)}"
)

# ❌ 나쁜 예시
logger.error("Error occurred")  # 정보 부족
```

### 4. 재시도 간격

```python
# ✅ 좋은 예시 - 지수 백오프 + 랜덤 지터
wait_time = (2 ** retry_count) + random.uniform(1, 3)

# ❌ 나쁜 예시 - 고정 간격
time.sleep(5)  # 서버에 부담
```

---

## 참고 자료

- [Circuit Breaker Pattern - Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Exponential Backoff - Google Cloud](https://cloud.google.com/iot/docs/how-tos/exponential-backoff)
- [Retry Pattern - Microsoft Azure](https://docs.microsoft.com/en-us/azure/architecture/patterns/retry)

---

**작성일**: 2025-11-02
**버전**: 1.0
**작성자**: Claude Code Assistant
