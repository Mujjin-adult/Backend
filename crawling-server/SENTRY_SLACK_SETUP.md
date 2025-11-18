# Sentry와 Slack 연동 가이드

크롤링 서버의 Sentry를 Slack과 연결하여 실시간 에러 알림을 받는 방법입니다.

---

## 📋 사전 준비

### 1. Sentry 프로젝트 생성

1. **Sentry 웹사이트 접속**: https://sentry.io/
2. **회원가입 또는 로그인**
3. **새 프로젝트 생성**:
   - Platform: Python
   - Project Name: `incheon-univ-crawler` (또는 원하는 이름)
   - Alert Frequency: 선택 (기본값 사용 권장)

4. **DSN 복사**:
   - 프로젝트 생성 후 표시되는 DSN을 복사
   - 형식: `https://[KEY]@[HOST]/[PROJECT_ID]`

### 2. 환경 변수 설정

크롤링 서버의 `.env` 파일 또는 Docker Compose에 Sentry DSN을 추가합니다.

**Option 1: `.env` 파일에 추가**

```bash
# Sentry 설정
SENTRY_DSN=https://your-key@o123456.ingest.sentry.io/7654321
ENV=production
APP_NAME=인천대학교 크롤링 서버
```

**Option 2: `docker-compose.yml`에 추가**

```yaml
services:
  fastapi:
    environment:
      - SENTRY_DSN=https://your-key@o123456.ingest.sentry.io/7654321
      - ENV=production
      - APP_NAME=인천대학교 크롤링 서버
```

---

## 🔗 Sentry와 Slack 연동

### Step 1: Sentry 대시보드 접속

1. https://sentry.io/ 로그인
2. 생성한 프로젝트 선택
3. 좌측 사이드바에서 **Settings** 클릭

### Step 2: Slack Integration 설치

1. **Settings** → **Integrations** 클릭
2. **Slack** 검색 또는 찾기
3. **Install** 버튼 클릭
4. **Add to Slack** 버튼 클릭
5. Slack 워크스페이스 선택 및 권한 승인

### Step 3: Slack Integration 설정

1. **Workspace** 선택
2. 알림을 받을 **Channel** 선택 (예: `#alerts`, `#crawler-errors`)
3. **Save Settings** 클릭

### Step 4: Alert Rules 설정

Alert Rules를 설정하여 어떤 에러가 발생했을 때 Slack 알림을 받을지 정의합니다.

#### 방법 1: 기본 Alert Rule 사용

1. **Alerts** → **Create Alert Rule** 클릭
2. 템플릿 선택:
   - **Issues**: 새로운 에러 발생 시 알림
   - **High Priority Issues**: 중요도가 높은 에러만 알림
   - **Custom**: 직접 조건 설정

#### 방법 2: 커스텀 Alert Rule 생성

1. **Alerts** → **Create Alert Rule** 클릭
2. **Alert Name**: 알림 이름 입력 (예: "크롤링 서버 에러 알림")

3. **When 조건 설정**:
   ```
   When an event is captured
   ```
   또는 특정 조건:
   ```
   When an issue's state changes from unresolved to resolved
   When error count is above 10 in 1 hour
   ```

4. **If 조건 설정** (선택사항):
   - Environment: `production`
   - Level: `error`, `fatal`
   - Tags:
     - `crawler.category`: `volunteer`, `scholarship` 등
     - `crawler.error_type`: `TemporaryError`, `PermanentError`

5. **Then 액션 설정**:
   - **Send a notification via**: Slack 선택
   - **Workspace**: 연결한 워크스페이스 선택
   - **Channel**: 알림 받을 채널 선택

6. **Save Rule** 클릭

---

## 🎨 추천 Alert Rules

### 1. 모든 에러 알림 (개발 초기)

```
Alert Name: [크롤링] 모든 에러 알림
When: An event is captured
If:
  - Environment: production
  - Level: error or fatal
Then: Send notification to #crawler-errors
```

### 2. 크롤러 에러만 알림

```
Alert Name: [크롤링] Crawler 에러
When: An event is captured
If:
  - Tag: crawler.category exists
  - Level: error
Then: Send notification to #crawler-errors
```

### 3. 반복되는 에러 알림 (노이즈 감소)

```
Alert Name: [크롤링] 빈번한 에러
When: An issue's frequency is above 10 in 1 hour
If:
  - Environment: production
Then: Send notification to #crawler-alerts
```

### 4. 중요 에러만 알림 (프로덕션)

```
Alert Name: [크롤링] 중요 에러
When: An event is captured
If:
  - Level: fatal
  - OR Tag: crawler.error_type equals "PermanentError"
Then: Send notification to #critical-alerts
```

---

## 📊 Slack 알림 예시

알림 메시지 형식:

```
🔴 [Error] 인천대학교 크롤링 서버

Exception: ConnectionError in college_crawl_task
Environment: production
Level: error

Tags:
  crawler.category: volunteer
  crawler.error_type: TemporaryError

Message: Failed to connect to https://www.inu.ac.kr

View on Sentry: https://sentry.io/...
```

---

## 🧪 테스트

### 1. 코드에서 테스트 에러 발생

크롤링 서버의 특정 엔드포인트를 호출하거나, Python 스크립트로 테스트:

```python
# test_sentry.py
import os
os.environ['SENTRY_DSN'] = 'your-dsn-here'

from sentry_config import init_sentry, capture_message_with_level, track_crawler_error

# Sentry 초기화
init_sentry()

# 테스트 메시지
capture_message_with_level("Sentry 테스트 메시지", level="info")

# 크롤러 에러 테스트
track_crawler_error(
    category="volunteer",
    error_type="TemporaryError",
    url="https://www.inu.ac.kr/test",
    exception=Exception("테스트 에러입니다")
)

print("테스트 이벤트를 Sentry로 전송했습니다!")
```

### 2. FastAPI 엔드포인트에서 테스트

임시로 에러를 발생시키는 엔드포인트 추가:

```python
# main.py에 추가
@app.get("/test-error")
async def test_error():
    """Sentry 테스트용 에러 발생"""
    from sentry_config import track_crawler_error

    track_crawler_error(
        category="test",
        error_type="TestError",
        url="http://localhost:8001/test-error",
        exception=Exception("Sentry와 Slack 연동 테스트입니다!")
    )

    return {"message": "테스트 에러를 Sentry로 전송했습니다!"}
```

실행:
```bash
curl http://localhost:8001/test-error
```

### 3. 확인

1. Sentry 대시보드에서 이벤트 확인
2. Slack 채널에서 알림 확인
3. 알림이 오지 않으면 Alert Rules 설정 재확인

---

## 🔧 고급 설정

### Slack 알림 커스터마이징

**Sentry → Settings → Integrations → Slack → Configure**에서:

1. **Notification Settings**:
   - 알림 형식 선택 (Simple, Detailed)
   - 멘션 설정 (`@channel`, `@here`, 특정 사용자)

2. **Channels**:
   - 여러 채널에 알림 설정 가능
   - 에러 레벨별로 다른 채널 사용

### 환경별 알림 분리

```
개발 환경: #dev-errors
스테이징: #staging-errors
프로덕션: #production-critical
```

Alert Rules에서 Environment 조건을 사용하여 분리

---

## 📈 모니터링 Best Practices

### 1. 알림 피로 방지

- 너무 많은 알림은 무시하게 됨
- 중요한 에러만 Slack 알림 설정
- 덜 중요한 에러는 Sentry 대시보드에서만 확인

### 2. 에러 그룹핑

Sentry는 유사한 에러를 자동으로 그룹화합니다.
- 같은 에러가 100번 발생해도 1개의 Issue로 표시
- Slack 알림도 1번만 발송 (설정에 따라)

### 3. 해결 워크플로우

1. Slack에서 에러 알림 확인
2. Sentry 링크 클릭하여 상세 정보 확인
3. 에러 수정 후 Sentry에서 **Resolve** 버튼 클릭
4. 같은 에러 재발 시 자동으로 **Regressed** 상태로 변경 및 재알림

---

## 🚀 다음 단계

### 1. Performance Monitoring

Sentry의 Performance 기능으로 API 응답 시간 모니터링:
- 느린 API 감지
- 병목 지점 파악
- 트랜잭션 추적

### 2. Cron 모니터링

Celery Beat 작업이 정상적으로 실행되는지 모니터링:
- 스케줄된 작업 실행 여부 확인
- 실패한 작업 알림

### 3. Release Tracking

배포 추적으로 어느 버전에서 에러가 발생했는지 확인:
```python
# sentry_config.py
release=f"{app_name}@{VERSION}"  # 현재 1.0.0
```

---

## 📞 문제 해결

### Q: Slack 알림이 오지 않아요

**확인사항**:
1. ✅ SENTRY_DSN이 올바르게 설정되었나요?
2. ✅ Sentry 대시보드에서 이벤트가 수신되고 있나요?
3. ✅ Alert Rule이 활성화되어 있나요?
4. ✅ Alert Rule의 조건이 너무 엄격하지 않나요?
5. ✅ Slack Integration이 올바른 채널에 연결되어 있나요?

### Q: 너무 많은 알림이 와요

**해결방법**:
1. Alert Rule의 조건을 더 구체적으로 설정
2. "Frequency" 조건 추가 (1시간에 X회 이상 발생 시)
3. 덜 중요한 에러는 `level="warning"`으로 낮춤

### Q: 특정 에러만 제외하고 싶어요

**Alert Rule에서 조건 추가**:
```
If:
  - Tag: error.type does not equal "ExpectedError"
  - OR Message does not contain "404 Not Found"
```

---

## 📚 참고 자료

- [Sentry Documentation](https://docs.sentry.io/)
- [Sentry Slack Integration](https://docs.sentry.io/product/integrations/notification-incidents/slack/)
- [Sentry Alert Rules](https://docs.sentry.io/product/alerts-notifications/alerts/)
- [Python SDK Reference](https://docs.sentry.io/platforms/python/)

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
