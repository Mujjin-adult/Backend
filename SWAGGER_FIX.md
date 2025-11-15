# Swagger UI 문제 해결 완료 ✅

## 🔧 수정한 내용

### 1. SecurityConfig.java 수정
모든 Swagger 관련 경로를 permitAll()에 추가:
```java
.requestMatchers(
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/swagger-resources/**",
    "/webjars/**",
    "/configuration/**"
).permitAll()
```

### 2. JwtAuthenticationFilter.java 수정
JWT 검증을 건너뛸 경로에 Swagger 경로 추가:
```java
path.startsWith("/swagger-ui/") ||
path.startsWith("/swagger-ui.html") ||
path.equals("/swagger-ui.html") ||
path.startsWith("/v3/api-docs") ||
path.startsWith("/swagger-resources/") ||
path.startsWith("/webjars/") ||
path.startsWith("/configuration/")
```

### 3. application.yml에 Swagger 설정 추가
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

---

## 🚀 Swagger UI 접속 방법

### Step 1: 애플리케이션 재시작
IntelliJ에서:
1. 실행 중인 애플리케이션 중지 (빨간색 정지 버튼)
2. `IncheonNoticeApplication.java` 다시 실행

### Step 2: Swagger UI 접속

다음 URL 중 하나로 접속:

**주 URL:**
```
http://localhost:8080/swagger-ui.html
```

**또는:**
```
http://localhost:8080/swagger-ui/index.html
```

### Step 3: API 문서 확인
브라우저에서 Swagger UI가 열리면 다음을 볼 수 있습니다:
- 모든 API 엔드포인트 목록
- 각 API의 요청/응답 스키마
- Try it out 버튼으로 직접 테스트 가능

---

## 📋 Swagger에서 볼 수 있는 API 목록

### 1. Auth Controller (인증)
- POST /api/auth/signup - 회원가입
- POST /api/auth/login - 로그인
- POST /api/auth/refresh - 토큰 갱신
- POST /api/auth/logout - 로그아웃
- GET /api/auth/verify-email - 이메일 인증
- POST /api/auth/resend-verification - 인증 메일 재발송
- POST /api/auth/forgot-password - 비밀번호 찾기
- POST /api/auth/reset-password - 비밀번호 재설정

### 2. User Controller (사용자)
- GET /api/users/me - 내 정보 조회
- PUT /api/users/me - 프로필 수정
- PUT /api/users/settings - 설정 변경
- PUT /api/users/password - 비밀번호 변경
- PUT /api/users/fcm-token - FCM 토큰 업데이트
- DELETE /api/users/me - 회원 탈퇴

### 3. Bookmark Controller (북마크)
- POST /api/bookmarks - 북마크 생성
- GET /api/bookmarks - 내 북마크 목록
- GET /api/bookmarks/{id} - 북마크 상세
- PUT /api/bookmarks/{id}/memo - 메모 수정
- DELETE /api/bookmarks/{id} - 북마크 삭제
- GET /api/bookmarks/check/{noticeId} - 북마크 여부 확인
- GET /api/bookmarks/count - 북마크 개수

### 4. User Preference Controller (환경설정)
- POST /api/preferences/categories - 카테고리 구독
- GET /api/preferences/categories - 내 구독 목록
- GET /api/preferences/categories/active - 활성 구독만
- PUT /api/preferences/categories/{categoryId}/notification - 알림 켜기/끄기
- DELETE /api/preferences/categories/{categoryId} - 구독 취소
- GET /api/preferences/categories/{categoryId}/subscribed - 구독 여부

### 5. Notice Controller (공지사항)
- GET /api/notices - 공지사항 목록
- GET /api/notices/{id} - 공지사항 상세
- GET /api/notices/search - 공지사항 검색
- GET /api/notices/category/{categoryCode} - 카테고리별 공지사항

### 6. Category Controller (카테고리)
- GET /api/categories - 카테고리 목록
- GET /api/categories/active - 활성 카테고리만
- GET /api/categories/{code} - 카테고리 상세

---

## 🔐 인증이 필요한 API 테스트 방법

### Step 1: 회원가입 & 로그인
1. Swagger에서 POST /api/auth/signup 실행
2. POST /api/auth/login으로 로그인
3. 응답에서 `accessToken` 값 복사

### Step 2: Authorization 설정
1. Swagger UI 우측 상단 **"Authorize"** 버튼 클릭
2. 팝업창에서 다음과 같이 입력:
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
   (실제 토큰 값으로 대체)
3. **Authorize** 버튼 클릭
4. **Close** 버튼 클릭

### Step 3: 인증된 API 호출
이제 자물쇠 아이콘이 열려있는 모든 API를 호출할 수 있습니다!

---

## ⚠️ 여전히 Swagger가 안 열린다면?

### 1. 애플리케이션 로그 확인
콘솔에서 다음 로그를 확인:
```
Started IncheonNoticeApplication in X.XXX seconds
```
이 메시지가 나왔다면 정상 실행된 것입니다.

### 2. 포트 확인
다른 애플리케이션이 8080 포트를 사용하고 있는지 확인:
```bash
lsof -i :8080
```

포트가 사용 중이면 application.yml의 포트 변경:
```yaml
server:
  port: 8081  # 다른 포트로 변경
```

### 3. 브라우저 캐시 삭제
- Chrome: Cmd+Shift+Delete
- 시크릿 모드로 접속 시도

### 4. 정확한 URL 확인
다음 URL들을 모두 시도:
- http://localhost:8080/swagger-ui.html
- http://localhost:8080/swagger-ui/index.html
- http://localhost:8080/swagger-ui/
- http://localhost:8080/v3/api-docs

### 5. 애플리케이션 완전 재시작
1. IntelliJ에서 중지
2. `./gradlew clean` (터미널에서)
3. IntelliJ에서 다시 실행

### 6. 의존성 확인
build.gradle에 다음이 있는지 확인:
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

---

## 🎯 테스트 시나리오 (Swagger UI에서)

### 시나리오 1: 회원가입부터 북마크까지

1. **POST /api/auth/signup**
   ```json
   {
     "studentId": "202012345",
     "email": "test@inu.ac.kr",
     "password": "password123",
     "name": "홍길동"
   }
   ```

2. **POST /api/auth/login**
   ```json
   {
     "email": "test@inu.ac.kr",
     "password": "password123"
   }
   ```
   → accessToken 복사 → Authorize 버튼 클릭 → 토큰 입력

3. **GET /api/users/me**
   → 내 정보 조회 확인

4. **PUT /api/users/settings**
   ```json
   {
     "darkMode": true,
     "systemNotificationEnabled": false
   }
   ```

5. **GET /api/categories**
   → 카테고리 목록 확인 (카테고리 ID 확인)

6. **POST /api/preferences/categories**
   ```json
   {
     "categoryId": 1,
     "notificationEnabled": true
   }
   ```

7. **POST /api/bookmarks** (공지사항이 있는 경우)
   ```json
   {
     "noticeId": 1,
     "memo": "중요!"
   }
   ```

8. **GET /api/bookmarks**
   → 내 북마크 목록 확인

---

## ✅ 성공 확인

Swagger UI가 열리고:
- [ ] 모든 컨트롤러가 보임 (Auth, User, Bookmark, Preference, Notice, Category)
- [ ] Try it out 버튼 동작
- [ ] 회원가입 API 호출 성공 (200 OK)
- [ ] 로그인 API 호출 성공 (accessToken 받음)
- [ ] Authorize 설정 후 인증 필요한 API 호출 성공

---

## 🆘 그래도 안 된다면

다음 정보를 확인해주세요:
1. 콘솔 로그 전체 (에러 메시지)
2. 접속 시도한 URL
3. 브라우저 개발자 도구 (F12) > Network 탭의 에러
4. IntelliJ의 Run 설정 (Environment variables 등)

이 정보를 바탕으로 추가 도움을 드릴 수 있습니다!
