# Phase 3: FCM 푸시 알림 시스템 구현 계획

## 📋 개요

Phase 1-2에서 완료된 기본 API를 기반으로 FCM(Firebase Cloud Messaging)을 이용한 푸시 알림 시스템을 구현합니다.

**목표**: 사용자가 구독한 카테고리의 신규 공지사항 발생 시 자동으로 푸시 알림 전송

## 🎯 구현 목표

### 1. 실시간 공지사항 알림
- 사용자가 구독한 카테고리에 신규 공지사항이 등록되면 자동으로 푸시 알림 전송
- 알림 설정을 끈 카테고리는 알림 전송하지 않음

### 2. 키워드 알림 (선택)
- 사용자가 설정한 키워드가 포함된 공지사항 발생 시 알림
- 예: "장학", "취업", "대외활동" 등

### 3. 알림 이력 관리
- 전송된 알림 이력을 데이터베이스에 저장
- 사용자별 알림 조회 API 제공

## 🏗️ 아키텍처

```
┌─────────────────────────────────────────┐
│         크롤링 서버 (FastAPI)            │
│  - 신규 공지사항 발견                    │
│  - Main 서버에 웹훅 호출                 │
└──────────────┬──────────────────────────┘
               │ HTTP POST /api/webhooks/new-notice
               ▼
┌─────────────────────────────────────────┐
│        메인 서버 (Spring Boot)           │
│  ┌──────────────────────────────────┐  │
│  │ WebhookController                │  │
│  │ - 신규 공지 수신                  │  │
│  │ - 알림 대상 사용자 조회            │  │
│  └─────────┬────────────────────────┘  │
│            │                            │
│  ┌─────────▼────────────────────────┐  │
│  │ NotificationService              │  │
│  │ - 알림 대상 필터링                │  │
│  │ - FCM 메시지 생성                │  │
│  │ - 알림 이력 저장                  │  │
│  └─────────┬────────────────────────┘  │
│            │                            │
│  ┌─────────▼────────────────────────┐  │
│  │ FCMService                       │  │
│  │ - Firebase Admin SDK 사용        │  │
│  │ - 푸시 알림 전송                  │  │
│  └──────────────────────────────────┘  │
└─────────────┬───────────────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │  Firebase Cloud     │
    │  Messaging (FCM)    │
    └─────────┬───────────┘
              │
              ▼
    ┌─────────────────────┐
    │   모바일 앱 (Flutter)│
    │   - 푸시 알림 수신   │
    └─────────────────────┘
```

## 📦 필요한 구성 요소

### 1. Firebase 프로젝트 설정
- [ ] Firebase 콘솔에서 프로젝트 생성
- [ ] FCM 활성화
- [ ] 서비스 계정 키(JSON) 다운로드
- [ ] Flutter 앱에 Firebase SDK 통합

### 2. Backend 구현 (Spring Boot)

#### 2.1 의존성 추가
```gradle
// build.gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

#### 2.2 Entity 추가
- **NotificationHistory** - 알림 전송 이력
  ```java
  - id: Long (PK)
  - user: User (FK)
  - notice: Notice (FK)
  - title: String
  - body: String
  - sentAt: LocalDateTime
  - isRead: Boolean
  ```

- **KeywordAlert** (선택) - 키워드 알림 설정
  ```java
  - id: Long (PK)
  - user: User (FK)
  - keyword: String
  - isActive: Boolean
  ```

#### 2.3 Service 구현
- **FCMService**
  - `sendNotification(fcmToken, title, body, data)` - 단일 알림 전송
  - `sendMultipleNotifications(tokens, title, body, data)` - 다중 알림 전송
  - `sendTopicNotification(topic, title, body, data)` - 토픽 기반 알림

- **NotificationService**
  - `notifyNewNotice(Notice notice)` - 신규 공지 알림
  - `findTargetUsers(Notice notice)` - 알림 대상 사용자 조회
  - `saveNotificationHistory()` - 알림 이력 저장
  - `getUserNotifications(User user)` - 사용자별 알림 이력 조회

#### 2.4 Controller 추가
- **WebhookController**
  - `POST /api/webhooks/new-notice` - 크롤링 서버로부터 신규 공지 수신

- **NotificationController**
  - `GET /api/notifications` - 내 알림 이력 조회
  - `PUT /api/notifications/{id}/read` - 알림 읽음 처리
  - `DELETE /api/notifications/{id}` - 알림 삭제

#### 2.5 Configuration
- **FCMConfig**
  - Firebase Admin SDK 초기화
  - 서비스 계정 키 로드

### 3. Crawling Server 수정 (FastAPI)

#### 3.1 웹훅 호출 추가
```python
# crawler.py
async def notify_new_notice(notice_data: dict):
    """신규 공지사항을 메인 서버에 알림"""
    async with httpx.AsyncClient() as client:
        await client.post(
            f"{BACKEND_URL}/api/webhooks/new-notice",
            json=notice_data,
            headers={"Authorization": f"Bearer {WEBHOOK_SECRET}"}
        )
```

#### 3.2 중복 체크 강화
- 이미 저장된 공지는 웹훅 호출하지 않음
- `external_id` 기반 중복 체크

### 4. Database Schema 업데이트

```sql
-- 알림 이력 테이블
CREATE TABLE notification_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    notice_id BIGINT REFERENCES notices(id),
    title VARCHAR(255) NOT NULL,
    body TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_notification_user_id ON notification_history(user_id);
CREATE INDEX idx_notification_sent_at ON notification_history(sent_at);

-- 키워드 알림 테이블 (선택)
CREATE TABLE keyword_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    keyword VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_keyword_alert_user_id ON keyword_alerts(user_id);
```

## 🔄 알림 흐름

### 시나리오 1: 신규 공지사항 알림

1. **크롤링 서버**: 신규 공지사항 발견
2. **크롤링 서버**: DB에 저장 (외부 ID 중복 체크)
3. **크롤링 서버**: 웹훅 호출 → `POST /api/webhooks/new-notice`
4. **메인 서버**: 웹훅 수신
5. **메인 서버**: 해당 카테고리를 구독한 사용자 조회
6. **메인 서버**: 알림 설정이 켜진 사용자만 필터링
7. **메인 서버**: FCM 토큰이 등록된 사용자만 필터링
8. **메인 서버**: FCM 푸시 알림 전송
9. **메인 서버**: 알림 이력 저장
10. **Flutter 앱**: 푸시 알림 수신 및 표시

### 시나리오 2: 키워드 알림 (선택)

1. **메인 서버**: 신규 공지사항 제목/내용 확인
2. **메인 서버**: 키워드 알림 설정 조회
3. **메인 서버**: 매칭되는 키워드가 있는 사용자에게 알림 전송

## 🧪 테스트 계획

### 1. 단위 테스트
- [ ] FCMService 테스트 (Mock 사용)
- [ ] NotificationService 테스트
- [ ] WebhookController 테스트

### 2. 통합 테스트
- [ ] 크롤링 서버 → 웹훅 → 알림 전송 전체 흐름 테스트
- [ ] 알림 대상 필터링 로직 테스트
- [ ] 알림 이력 저장 테스트

### 3. 수동 테스트
- [ ] Firebase 콘솔에서 테스트 메시지 전송
- [ ] Postman으로 웹훅 API 테스트
- [ ] Flutter 앱에서 실제 푸시 알림 수신 확인

## 📝 API 명세

### Webhook API

#### POST /api/webhooks/new-notice
신규 공지사항 알림 (크롤링 서버 전용)

**Request:**
```json
{
  "noticeId": 123,
  "title": "[장학] 2024년 2학기 장학금 신청 안내",
  "categoryCode": "SCHOLARSHIP",
  "publishedAt": "2024-11-17T10:00:00"
}
```

**Response:**
```json
{
  "success": true,
  "message": "알림이 전송되었습니다",
  "data": {
    "targetUserCount": 45,
    "sentCount": 42
  }
}
```

### Notification API

#### GET /api/notifications
내 알림 이력 조회 (페이징)

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `unreadOnly` (default: false)

**Response:**
```json
{
  "success": true,
  "message": "알림 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "[장학] 신규 장학금 공지",
        "body": "2024년 2학기 장학금 신청 안내",
        "noticeId": 123,
        "sentAt": "2024-11-17T10:05:00",
        "isRead": false
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

#### PUT /api/notifications/{id}/read
알림 읽음 처리

#### DELETE /api/notifications/{id}
알림 삭제

## 🔐 보안 고려사항

### 1. 웹훅 보안
- Authorization 헤더로 비밀 토큰 전송
- IP 화이트리스트 (크롤링 서버 IP만 허용)
- Rate Limiting (DDoS 방지)

### 2. FCM 토큰 보안
- HTTPS 통신 필수
- 토큰은 암호화하여 저장 (권장)
- 토큰 갱신 로직 구현

### 3. 개인정보 보호
- 알림 내용은 최소한의 정보만 포함
- 상세 내용은 앱 내에서 조회

## ⚙️ 환경 변수

```yaml
# application.yml
firebase:
  credentials-path: ${FIREBASE_CREDENTIALS_PATH:/path/to/serviceAccountKey.json}
  project-id: ${FIREBASE_PROJECT_ID:your-project-id}

webhook:
  secret: ${WEBHOOK_SECRET:your-secret-key}
  allowed-ips: ${WEBHOOK_ALLOWED_IPS:localhost,127.0.0.1}
```

## 📅 구현 일정 (예상)

### Week 1: 기반 구축
- [ ] Firebase 프로젝트 설정 (1일)
- [ ] FCMService 구현 및 테스트 (2일)
- [ ] Entity 및 Repository 생성 (1일)
- [ ] Database Schema 적용 (1일)

### Week 2: 핵심 기능 구현
- [ ] NotificationService 구현 (2일)
- [ ] WebhookController 구현 (1일)
- [ ] 크롤링 서버 웹훅 연동 (1일)
- [ ] 통합 테스트 (1일)

### Week 3: 추가 기능 및 최적화
- [ ] NotificationController 구현 (1일)
- [ ] 키워드 알림 구현 (선택, 2일)
- [ ] 성능 최적화 (배치 전송 등) (1일)
- [ ] 문서화 및 테스트 (1일)

## 🚀 다음 단계

1. **Firebase 프로젝트 생성**
   - https://console.firebase.google.com/
   - 프로젝트 생성 → FCM 활성화
   - 서비스 계정 키 다운로드

2. **Flutter 앱 FCM 설정**
   - `firebase_messaging` 패키지 추가
   - FCM 토큰 획득 로직 구현
   - 백그라운드 메시지 핸들러 구현

3. **Backend 구현 시작**
   - Firebase Admin SDK 의존성 추가
   - FCMConfig 및 FCMService 구현
   - 테스트 메시지 전송 성공 확인

## 📚 참고 자료

- [Firebase Cloud Messaging 문서](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Admin Java SDK](https://firebase.google.com/docs/admin/setup)
- [Flutter firebase_messaging](https://pub.dev/packages/firebase_messaging)
- `FCM_GUIDE.md` - 기존 FCM 가이드 문서

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
