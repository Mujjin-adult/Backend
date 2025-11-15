# API 테스트 가이드

## 🚀 실행 전 체크리스트

### 1. Docker 컨테이너 확인
- ✅ PostgreSQL (포트 5432) - 실행 중
- ✅ Redis (포트 6379) - 실행 중
- ✅ pgAdmin (포트 5050) - 실행 중

### 2. 환경 변수 설정 (선택사항)
이메일 기능을 테스트하려면 다음 환경 변수 설정 필요:
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

**Gmail 앱 비밀번호 생성 방법:**
1. Google 계정 관리 > 보안
2. 2단계 인증 활성화
3. 앱 비밀번호 생성
4. 생성된 16자리 비밀번호를 MAIL_PASSWORD에 설정

이메일 기능을 테스트하지 않으려면 환경 변수 설정 없이 진행 가능 (에러 로그만 발생)

---

## 📍 접속 URL

### API 문서 (Swagger UI)
```
http://localhost:8080/swagger-ui.html
또는
http://localhost:8080/swagger-ui/index.html
```

### pgAdmin (데이터베이스 관리)
```
http://localhost:5050
```
- 이메일: `admin@admin.com`
- 비밀번호: `admin`

**서버 연결 설정:**
1. pgAdmin 좌측 Servers 우클릭 > Create > Server
2. General 탭: Name = `Incheon Notice DB`
3. Connection 탭:
   - Host: `postgres` (Docker 네트워크 내에서) 또는 `localhost`
   - Port: `5432`
   - Database: `incheon_notice`
   - Username: `postgres`
   - Password: `postgres`

---

## 🧪 API 테스트 시나리오

### Step 1: 회원가입 (POST /api/auth/signup)

**요청 예시:**
```json
{
  "studentId": "202012345",
  "email": "test@inu.ac.kr",
  "password": "password123",
  "name": "홍길동"
}
```

**확인 사항:**
- ✅ 응답 200 OK
- ✅ 회원가입 성공 메시지
- ✅ pgAdmin에서 `users` 테이블에 데이터 확인
- ✅ `email_verification_tokens` 테이블에 토큰 생성 확인
- ⚠️ 이메일 발송 (SMTP 설정한 경우만)

---

### Step 2: 로그인 (POST /api/auth/login)

**요청 예시:**
```json
{
  "email": "test@inu.ac.kr",
  "password": "password123"
}
```

**응답에서 받은 accessToken 복사:**
- Swagger UI 우측 상단 "Authorize" 버튼 클릭
- `Bearer {accessToken}` 입력
- 이제 인증이 필요한 API 사용 가능!

---

### Step 3: 내 정보 조회 (GET /api/users/me)

**확인 사항:**
- ✅ 사용자 정보 조회 성공
- ✅ darkMode, systemNotificationEnabled 필드 확인

---

### Step 4: 사용자 설정 변경 (PUT /api/users/settings)

**요청 예시:**
```json
{
  "darkMode": true,
  "systemNotificationEnabled": false
}
```

**확인 사항:**
- ✅ 설정 변경 성공
- ✅ pgAdmin에서 `users` 테이블 업데이트 확인

---

### Step 5: 카테고리 조회 (GET /api/categories)

카테고리 데이터가 없다면 먼저 삽입 필요:

**pgAdmin에서 실행:**
```sql
INSERT INTO categories (code, name, type, url, is_active, description, created_at, updated_at) VALUES
('scholarship', '장학공지', 'ADMINISTRATIVE', 'https://www.inu.ac.kr/user/indexSub.do?codyMenuSeq=56955', true, '장학금 관련 공지', NOW(), NOW()),
('academic', '학사공지', 'ADMINISTRATIVE', 'https://www.inu.ac.kr/user/indexSub.do?codyMenuSeq=56954', true, '학사 관련 공지', NOW(), NOW()),
('volunteer', '봉사공지', 'ADMINISTRATIVE', 'https://www.inu.ac.kr/user/indexSub.do?codyMenuSeq=56956', true, '봉사활동 공지', NOW(), NOW());
```

---

### Step 6: 카테고리 구독 (POST /api/preferences/categories)

**요청 예시:**
```json
{
  "categoryId": 1,
  "notificationEnabled": true
}
```

**확인 사항:**
- ✅ 구독 성공
- ✅ pgAdmin에서 `user_preferences` 테이블 확인

---

### Step 7: 내 구독 목록 조회 (GET /api/preferences/categories)

**확인 사항:**
- ✅ 구독한 카테고리 목록 조회
- ✅ category 정보 포함

---

### Step 8: 공지사항 조회 (GET /api/notices)

공지사항 데이터가 없다면 크롤링 서버 실행 필요:

**크롤링 실행 (별도 터미널):**
```bash
cd crawling-server
python -m uvicorn app.main:app --reload --port 8001
```

그런 다음 크롤러 실행:
```bash
curl -X POST "http://localhost:8001/run-crawler/scholarship?api_key=your-api-key"
```

---

### Step 9: 북마크 생성 (POST /api/bookmarks)

**요청 예시:**
```json
{
  "noticeId": 1,
  "memo": "중요한 장학금 공지!"
}
```

**확인 사항:**
- ✅ 북마크 생성 성공
- ✅ pgAdmin에서 `bookmarks` 테이블 확인

---

### Step 10: 내 북마크 목록 (GET /api/bookmarks)

**확인 사항:**
- ✅ 북마크 목록 조회
- ✅ 공지사항 정보 포함
- ✅ 메모 확인

---

### Step 11: 비밀번호 찾기 (POST /api/auth/forgot-password)

**요청 예시:**
```json
{
  "email": "test@inu.ac.kr"
}
```

**확인 사항:**
- ✅ 재설정 메일 발송 메시지
- ✅ pgAdmin에서 `password_reset_tokens` 테이블에 토큰 생성 확인
- ⚠️ 이메일 수신 (SMTP 설정한 경우만)

---

## 🗄️ pgAdmin 데이터베이스 확인

### 주요 테이블 확인 쿼리

**1. 전체 사용자 조회:**
```sql
SELECT id, student_id, email, name, role, is_active, dark_mode,
       system_notification_enabled, is_email_verified, created_at
FROM users
ORDER BY created_at DESC;
```

**2. 이메일 인증 토큰 확인:**
```sql
SELECT t.id, t.token, t.expiry_date, t.used, t.verified_at,
       u.email, u.name
FROM email_verification_tokens t
JOIN users u ON t.user_id = u.id
ORDER BY t.created_at DESC;
```

**3. 사용자 환경설정 조회:**
```sql
SELECT up.id, u.email, c.name as category_name,
       up.notification_enabled, up.created_at
FROM user_preferences up
JOIN users u ON up.user_id = u.id
JOIN categories c ON up.category_id = c.id
ORDER BY up.created_at DESC;
```

**4. 북마크 조회:**
```sql
SELECT b.id, u.email, cn.title, b.memo, b.created_at
FROM bookmarks b
JOIN users u ON b.user_id = u.id
JOIN crawl_notice cn ON b.crawl_notice_id = cn.id
ORDER BY b.created_at DESC;
```

**5. 비밀번호 재설정 토큰 확인:**
```sql
SELECT t.id, t.token, t.expiry_date, t.used, t.used_at,
       u.email, u.name
FROM password_reset_tokens t
JOIN users u ON t.user_id = u.id
ORDER BY t.created_at DESC;
```

---

## ⚠️ 주요 확인 사항

### 1. 데이터베이스 스키마 자동 생성
- JPA `ddl-auto: update` 설정으로 테이블 자동 생성
- 새로운 엔티티 추가 시 자동으로 테이블 생성됨
- 확인: pgAdmin에서 Tables 확인

### 2. 새로 추가된 테이블
- ✅ `email_verification_tokens` - 이메일 인증 토큰
- ✅ `password_reset_tokens` - 비밀번호 재설정 토큰

### 3. User 테이블 새 컬럼
- ✅ `dark_mode` - 다크 모드 설정
- ✅ `system_notification_enabled` - 시스템 알림 허용
- ✅ `is_email_verified` - 이메일 인증 여부

---

## 🐛 트러블슈팅

### 문제: 이메일 발송 실패
**원인:** SMTP 설정 없음
**해결:** 환경 변수 설정 또는 application.yml 수정

### 문제: JWT 토큰 만료
**해결:** 다시 로그인해서 새 토큰 발급

### 문제: 카테고리/공지사항 데이터 없음
**해결:** pgAdmin에서 수동 삽입 또는 크롤링 서버 실행

### 문제: 데이터베이스 연결 실패
**해결:** Docker 컨테이너 확인
```bash
docker-compose ps
docker-compose up -d  # 필요시 재시작
```

---

## 📊 테스트 결과 체크리스트

- [ ] 회원가입 성공
- [ ] 로그인 성공 및 JWT 토큰 발급
- [ ] 사용자 정보 조회
- [ ] 사용자 설정 변경 (다크모드, 알림)
- [ ] 카테고리 조회
- [ ] 카테고리 구독
- [ ] 구독 목록 조회
- [ ] 알림 설정 변경
- [ ] 북마크 생성
- [ ] 북마크 목록 조회
- [ ] 북마크 메모 수정
- [ ] 북마크 삭제
- [ ] 비밀번호 찾기 요청
- [ ] pgAdmin에서 모든 데이터 확인

---

## 🎉 성공 기준

모든 API가 정상 작동하고, pgAdmin에서 데이터가 올바르게 저장/조회되면 성공!
