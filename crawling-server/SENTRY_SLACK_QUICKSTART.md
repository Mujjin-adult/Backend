# Sentry + Slack 빠른 시작 가이드 (5분)

크롤링 서버의 에러를 Slack으로 실시간 알림 받는 가장 빠른 방법입니다.

---

## ⚡ 빠른 시작 (5분)

### Step 1: Sentry 프로젝트 생성 (2분)

1. **https://sentry.io** 접속
2. **회원가입** (GitHub 계정으로 가능)
3. **Create Project** 클릭
   - Platform: **Python** 선택
   - Project Name: `incheon-univ-crawler` 입력
   - **Create Project** 클릭

4. **DSN 복사**
   - 표시되는 DSN 전체를 복사
   - 예시: `https://abc123def456@o123456.ingest.sentry.io/7654321`

### Step 2: 환경 변수 설정 (1분)

**크롤링 서버 디렉토리로 이동**:
```bash
cd /Users/chosunghoon/Desktop/Incheon_univ_noti_app/crawling-server
```

**`.env` 파일 편집** (없으면 생성):
```bash
# .env 파일에 추가
SENTRY_DSN=https://abc123def456@o123456.ingest.sentry.io/7654321
ENV=production
APP_NAME=인천대학교 크롤링 서버
```

**Docker Compose 재시작**:
```bash
docker-compose restart fastapi
```

### Step 3: Sentry 동작 확인 (30초)

**테스트 API 호출**:
```bash
curl http://localhost:8001/test-sentry
```

**Sentry 대시보드 확인**:
1. https://sentry.io 접속
2. 프로젝트 선택
3. **Issues** 탭에서 방금 전송된 에러 확인
4. ✅ 에러가 보이면 성공!

### Step 4: Slack 연동 (2분)

**Sentry에서 Slack 연결**:
1. Sentry 대시보드 → **Settings** → **Integrations**
2. **Slack** 검색 → **Install** 클릭
3. **Add to Slack** 버튼 클릭
4. Slack 워크스페이스 선택 및 권한 승인

**Alert Rule 생성**:
1. **Alerts** → **Create Alert Rule** 클릭
2. **Alert Name**: `크롤링 서버 에러 알림` 입력
3. **When**: `An event is captured` 선택
4. **If**:
   - Environment: `production` 선택
   - Level: `error` 또는 `fatal` 선택
5. **Then**:
   - **Send a notification via**: Slack 선택
   - **Channel**: 알림 받을 채널 선택 (예: `#alerts`)
6. **Save Rule** 클릭

### Step 5: Slack 알림 테스트 (30초)

**다시 테스트 API 호출**:
```bash
curl http://localhost:8001/test-sentry
```

**Slack 채널 확인**:
- 선택한 채널에 에러 알림이 와야 함
- ✅ 알림이 오면 완료!

---

## 🎯 완료!

이제 크롤링 서버에서 에러가 발생하면 자동으로 Slack 알림을 받을 수 있습니다!

---

## 📱 알림 예시

Slack에 다음과 같은 형식으로 알림이 옵니다:

```
🔴 [Error] 인천대학교 크롤링 서버

Exception: 🧪 Sentry와 Slack 연동 테스트입니다!
Environment: production
Level: error

Tags:
  crawler.category: test
  crawler.error_type: TestError

View on Sentry: https://sentry.io/...
```

---

## 🔧 자주 발생하는 문제

### Q: Slack 알림이 오지 않아요

**체크리스트**:
1. ✅ Sentry DSN이 `.env` 파일에 올바르게 입력되었나요?
2. ✅ Docker Compose를 재시작했나요?
3. ✅ Sentry 대시보드에서 이벤트가 수신되고 있나요?
4. ✅ Alert Rule이 **활성화** 상태인가요?
5. ✅ Alert Rule의 Environment가 `production`으로 설정되어 있나요?
6. ✅ Slack Integration이 올바른 채널에 연결되어 있나요?

**가장 흔한 원인**:
- Alert Rule의 조건이 너무 엄격함
- 잘못된 Slack 채널 선택
- Alert Rule이 비활성화 상태

**해결 방법**:
1. Sentry → Alerts → Rules에서 Rule 확인
2. Rule을 클릭하여 상태가 **Active**인지 확인
3. 조건을 단순화: Environment 조건 제거하고 테스트

### Q: Sentry에 이벤트가 수신되지 않아요

**확인사항**:
```bash
# 1. Docker Compose 로그 확인
docker-compose logs fastapi | grep -i sentry

# 2. 환경 변수 확인
docker-compose exec fastapi env | grep SENTRY

# 3. .env 파일 확인
cat .env | grep SENTRY
```

**해결 방법**:
1. SENTRY_DSN이 올바른지 확인
2. DSN 앞뒤에 공백이나 따옴표가 없는지 확인
3. Docker Compose 재시작: `docker-compose restart fastapi`

---

## 📚 다음 단계

### 1. 추가 Alert Rules 설정

**빈번한 에러 알림** (노이즈 감소):
```
Alert Name: 빈번한 에러
When: An issue's frequency is above 10 in 1 hour
Then: Send to #critical-alerts
```

**크롤러 에러만 알림**:
```
Alert Name: 크롤러 에러
When: An event is captured
If: Tag crawler.category exists
Then: Send to #crawler-errors
```

### 2. 에러 레벨별 채널 분리

```
Fatal 에러 → #critical-alerts (즉시 대응 필요)
Error → #errors (일반 에러)
Warning → Sentry 대시보드만 (알림 없음)
```

### 3. 성능 모니터링

Sentry의 Performance 기능 활성화:
- API 응답 시간 추적
- 느린 엔드포인트 발견
- 병목 지점 파악

---

## 💡 유용한 팁

### 1. 에러 그룹핑

Sentry는 유사한 에러를 자동으로 그룹화합니다.
- 같은 에러가 100번 발생해도 1개의 Issue
- Slack 알림도 1번만 (Frequency 설정에 따라)

### 2. 알림 스누즈

Slack 알림에서 **Snooze** 버튼 클릭:
- 일시적으로 알림 중단
- 나중에 다시 확인 가능

### 3. 에러 해결 워크플로우

```
1. Slack에서 에러 알림 확인
   ↓
2. "View on Sentry" 링크 클릭
   ↓
3. 스택 트레이스 및 컨텍스트 확인
   ↓
4. 에러 수정
   ↓
5. Sentry에서 "Resolve" 버튼 클릭
   ↓
6. 같은 에러 재발 시 자동으로 "Regressed" 알림
```

---

## 🎓 더 자세한 내용

상세한 설정 및 고급 기능은 **SENTRY_SLACK_SETUP.md** 파일을 참고하세요.

---

## 🆘 도움이 필요하신가요?

**Sentry 공식 문서**:
- [Slack Integration](https://docs.sentry.io/product/integrations/notification-incidents/slack/)
- [Alert Rules](https://docs.sentry.io/product/alerts-notifications/alerts/)
- [Python SDK](https://docs.sentry.io/platforms/python/)

**문제 해결**:
- Sentry Community: https://discord.gg/sentry
- GitHub Issues: https://github.com/getsentry/sentry/issues

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
