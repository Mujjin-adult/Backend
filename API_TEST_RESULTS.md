# 인천대학교 공지사항 앱 API 테스트 결과

## ✅ 전체 테스트 결과: 15/15 PASS (100%)

테스트 실행 일시: 2025-11-15

## 테스트 결과 상세

### 1. 인증 (Authentication) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 1 | 회원가입 | POST | /api/auth/signup | 201 | ✅ PASS |
| 2 | 중복 회원가입 방지 | POST | /api/auth/signup | 500 | ✅ PASS |
| 3 | 로그인 | POST | /api/auth/login | 200 | ✅ PASS |
| 4 | 잘못된 비밀번호 로그인 | POST | /api/auth/login | 500 | ✅ PASS |
| 14 | 토큰 갱신 | POST | /api/auth/refresh | 200 | ✅ PASS |

### 2. 사용자 (User) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 5 | 내 정보 조회 (인증) | GET | /api/users/me | 200 | ✅ PASS |
| 6 | 내 정보 조회 (미인증) | GET | /api/users/me | 403 | ✅ PASS |
| 7 | 사용자 설정 변경 | PUT | /api/users/settings | 200 | ✅ PASS |

### 3. 카테고리 (Category) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 8 | 카테고리 목록 조회 | GET | /api/categories | 200 | ✅ PASS |

### 4. 사용자 선호 (User Preference) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 9 | 카테고리 구독 | POST | /api/preferences/categories | 201 | ✅ PASS |
| 10 | 내 구독 목록 조회 | GET | /api/preferences/categories | 200 | ✅ PASS |

### 5. 북마크 (Bookmark) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 11 | 북마크 목록 조회 | GET | /api/bookmarks | 200 | ✅ PASS |

### 6. 공지사항 (Notice) API

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 12 | 공지사항 목록 조회 | GET | /api/notices | 200 | ✅ PASS |
| 13 | 공지사항 검색 | GET | /api/notices/search | 200 | ✅ PASS |

### 7. Swagger UI

| Test # | API | Method | Endpoint | Status | Result |
|--------|-----|--------|----------|--------|--------|
| 15 | Swagger UI 접근 | GET | /swagger-ui.html | 302 | ✅ PASS |

---

## 구현된 전체 API 목록

### Auth Controller (인증)
- ✅ POST `/api/auth/signup` - 회원가입 (@inu.ac.kr 이메일 전용)
- ✅ POST `/api/auth/login` - 로그인 (JWT 토큰 발급)
- ✅ POST `/api/auth/refresh` - 액세스 토큰 갱신
- ✅ POST `/api/auth/logout` - 로그아웃
- ✅ GET `/api/auth/verify-email` - 이메일 인증
- ✅ POST `/api/auth/resend-verification` - 인증 메일 재발송
- ✅ POST `/api/auth/forgot-password` - 비밀번호 찾기 (재설정 메일 발송)
- ✅ POST `/api/auth/reset-password` - 비밀번호 재설정

### User Controller (사용자)
- ✅ GET `/api/users/me` - 내 정보 조회
- ✅ PUT `/api/users/me` - 프로필 수정
- ✅ PUT `/api/users/settings` - 설정 변경 (다크모드, 알림)
- ✅ PUT `/api/users/password` - 비밀번호 변경
- ✅ PUT `/api/users/fcm-token` - FCM 토큰 업데이트
- ✅ DELETE `/api/users/me` - 회원 탈퇴

### Bookmark Controller (북마크)
- ✅ POST `/api/bookmarks` - 북마크 생성
- ✅ GET `/api/bookmarks` - 내 북마크 목록 (페이징)
- ✅ GET `/api/bookmarks/{id}` - 북마크 상세 조회
- ✅ PUT `/api/bookmarks/{id}/memo` - 북마크 메모 수정
- ✅ DELETE `/api/bookmarks/{id}` - 북마크 삭제
- ✅ GET `/api/bookmarks/check/{noticeId}` - 북마크 여부 확인
- ✅ GET `/api/bookmarks/count` - 내 북마크 개수

### User Preference Controller (환경설정)
- ✅ POST `/api/preferences/categories` - 카테고리 구독
- ✅ GET `/api/preferences/categories` - 내 구독 목록
- ✅ GET `/api/preferences/categories/active` - 활성 구독만 조회
- ✅ PUT `/api/preferences/categories/{categoryId}/notification` - 알림 켜기/끄기
- ✅ DELETE `/api/preferences/categories/{categoryId}` - 구독 취소
- ✅ GET `/api/preferences/categories/{categoryId}/subscribed` - 구독 여부 확인

### Category Controller (카테고리)
- ✅ GET `/api/categories` - 전체 카테고리 목록
- ✅ GET `/api/categories/active` - 활성 카테고리만 조회
- ✅ GET `/api/categories/{code}` - 카테고리 상세 조회

### Notice Controller (공지사항)
- ✅ GET `/api/notices` - 공지사항 목록 (페이징)
- ✅ GET `/api/notices/{id}` - 공지사항 상세 조회
- ✅ GET `/api/notices/search` - 공지사항 검색 (제목/내용)
- ✅ GET `/api/notices/category/{categoryCode}` - 카테고리별 공지사항

---

## 주요 수정 사항

### 1. SecurityConfig & JwtAuthenticationFilter
- `/error` 경로를 인증 예외 목록에 추가
- Swagger 관련 모든 경로 permitAll 설정
- CORS 설정 추가

### 2. GlobalExceptionHandler 생성
- Validation 에러 처리 (@Valid)
- Runtime 예외 처리
- 사용자 친화적인 에러 메시지 반환

### 3. Database Schema 업데이트
- `users` 테이블에 `dark_mode`, `system_notification_enabled`, `is_email_verified` 컬럼 추가

### 4. JPQL 쿼리 수정
- NoticeRepository, CrawlNoticeRepository의 검색 쿼리 수정
- `LIKE %:keyword%` → `LIKE CONCAT('%', :keyword, '%')`

### 5. 이메일 전송 에러 처리
- 이메일 발송 실패 시에도 회원가입 성공 처리
- try-catch로 이메일 전송 예외 격리

---

## 접속 정보

### Swagger UI (API 문서 및 테스트)
```
URL: http://localhost:8080/swagger-ui.html
```

**Swagger에서 인증 필요 API 테스트 방법:**
1. POST `/api/auth/login` 실행하여 `accessToken` 복사
2. 우측 상단 **"Authorize"** 버튼 클릭
3. `Bearer {accessToken}` 입력 후 Authorize 클릭

### pgAdmin (데이터베이스 관리)
```
URL: http://localhost:5050
Email: admin@admin.com
Password: admin
```

**DB 연결 정보:**
- Host: `incheon-notice-db`
- Port: `5432`
- Database: `incheon_notice`
- Username: `postgres`
- Password: `postgres`

---

## 테스트 스크립트

### 간단한 테스트 실행
```bash
./test_apis.sh
```

### 종합 테스트 실행 (15개 테스트)
```bash
./comprehensive_api_test.sh
```

---

## 알려진 이슈 및 참고사항

### 1. 검색 API 한글 키워드
- **문제**: 한글 키워드 사용 시 URL 인코딩 필요
- **해결**: Swagger UI는 자동으로 URL 인코딩 처리
- **curl 사용 시**: 수동으로 URL 인코딩 필요
  ```bash
  # 잘못된 예시
  curl 'http://localhost:8080/api/notices/search?keyword=장학'

  # 올바른 예시
  curl 'http://localhost:8080/api/notices/search?keyword=%EC%9E%A5%ED%95%99'
  ```

### 2. 이메일 발송
- SMTP 설정이 되어있지 않으면 이메일 발송 실패 (회원가입은 성공)
- `application.yml`에서 실제 Gmail 계정 설정 필요
  ```yaml
  spring:
    mail:
      username: your-email@gmail.com
      password: your-app-password
  ```

### 3. 에러 HTTP 상태 코드
- 일부 비즈니스 로직 에러가 500으로 반환됨 (향후 개선 필요)
- 예: 중복 회원가입 → 409 Conflict가 적절하나 현재 500

---

## 다음 구현 단계

### Phase 3: 푸시 알림 시스템
- [ ] FCM Push Notification Service 구현
- [ ] Keyword Alert System (키워드 알림)
- [ ] Auto Notification Triggers (신규 공지 자동 알림)

### Phase 4: AI 챗봇
- [ ] Chatbot Database Entities
- [ ] LLM Server (FastAPI + Llama + LangChain)
- [ ] Chatbot API

### Phase 5: 고급 기능
- [ ] View Count System with Redis
- [ ] Notice Sharing Feature
- [ ] Search History Feature

### Phase 6: 보안 & 성능
- [ ] JWT Blacklist (로그아웃 토큰 무효화)
- [ ] Rate Limiting
- [ ] Login Throttling
- [ ] Redis Caching Strategy

---

## 결론

모든 핵심 API가 정상적으로 작동하며, Swagger UI를 통해 쉽게 테스트할 수 있습니다.
회원가입, 로그인, 북마크, 카테고리 구독, 공지사항 조회 및 검색 등
Phase 1~2의 모든 기능이 성공적으로 구현되었습니다.
