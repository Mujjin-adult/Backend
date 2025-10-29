# Swagger API 문서 사용 가이드

코드 작성 없이 브라우저에서 클릭만으로 API를 테스트할 수 있습니다! 🚀

---

## 📌 1. Swagger UI 접속

### 브라우저에서 다음 주소 열기:

```
http://localhost:8080/swagger-ui/index.html
```

**⚠️ 중요:** 반드시 `/swagger-ui/index.html` 경로를 사용하세요!

### 화면 설명

Swagger UI가 열리면 다음과 같은 화면이 보입니다:

```
┌─────────────────────────────────────────┐
│ 인천대학교 공지사항 API                     │
│ 인천대학교 공지사항 앱 백엔드 API 문서      │
│ Version: 1.0.0                           │
├─────────────────────────────────────────┤
│ 🔓 Authorize                             │ ← 인증 버튼 (중요!)
├─────────────────────────────────────────┤
│ auth-controller                          │ ← API 그룹
│   POST /api/auth/signup                  │
│   POST /api/auth/login                   │
│   POST /api/auth/refresh                 │
└─────────────────────────────────────────┘
```

---

## 🎯 2. 회원가입 테스트 (인증 불필요)

### Step 1: API 선택

**auth-controller** 섹션을 펼치고 **POST /api/auth/signup** 클릭

### Step 2: Try it out 버튼 클릭

오른쪽 상단의 **"Try it out"** 버튼 클릭

### Step 3: 요청 데이터 입력

**Request body**에 다음과 같은 JSON 데이터가 표시됩니다:

```json
{
  "studentId": "string",
  "email": "string",
  "password": "string",
  "name": "string"
}
```

**예시 데이터로 변경:**

```json
{
  "studentId": "202412345",
  "email": "swagger@inu.ac.kr",
  "password": "password123",
  "name": "스웨거테스트"
}
```

### Step 4: 실행

파란색 **"Execute"** 버튼 클릭

### Step 5: 응답 확인

아래 **Responses** 섹션에서 결과 확인:

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다",
  "data": {
    "id": 5,
    "studentId": "202412345",
    "email": "swagger@inu.ac.kr",
    "name": "스웨거테스트",
    "role": "USER"
  },
  "timestamp": "2025-10-26T16:30:00.123456"
}
```

**✅ 성공! 사용자가 데이터베이스에 저장되었습니다!**

---

## 🔐 3. 로그인 및 JWT 토큰 받기

### Step 1: 로그인 API 실행

**POST /api/auth/login** 클릭 → **"Try it out"**

### Step 2: 로그인 정보 입력

```json
{
  "email": "swagger@inu.ac.kr",
  "password": "password123"
}
```

**💡 fcmToken 필드는 선택사항입니다!**
- 푸시 알림이 필요 없다면 입력하지 않아도 됩니다
- 테스트 목적이라면 더미 값을 입력할 수 있습니다: `"fcmToken": "test-token"`
- 실제 앱에서는 Firebase SDK가 자동으로 발급합니다

### Step 3: Execute 버튼 클릭

### Step 4: 응답에서 토큰 복사

응답 예시:

```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzd2FnZ2VyQGludS5hYy5rciIsImlhdCI6MTczNDYxNzQ2MywiZXhwIjoxNzM0NzAzODYzfQ.P5QZ...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzd2FnZ2VyQGludS5hYy5rciIsImlhdCI6MTczNDYxNzQ2MywiZXhwIjoxNzM1MjIyMjYzfQ.Kx4W...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 5,
      "studentId": "202412345",
      "email": "swagger@inu.ac.kr",
      "name": "스웨거테스트",
      "role": "USER"
    }
  }
}
```

**중요! `accessToken` 값을 복사하세요:**
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzd2FnZ2VyQGludS5hYy5rciIsImlhdCI6MTczNDYxNzQ2MywiZXhwIjoxNzM0NzAzODYzfQ.P5QZ...
```

---

## 🔓 4. 인증 설정 (JWT 토큰 등록)

로그인 후 받은 토큰을 Swagger에 등록하면 인증이 필요한 API를 사용할 수 있습니다.

### Step 1: Authorize 버튼 클릭

페이지 상단 오른쪽의 **🔓 Authorize** 버튼 클릭

### Step 2: 토큰 입력

**Available authorizations** 창이 열립니다.

**Value 입력란에 다음 형식으로 입력:**

```
Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzd2FnZ2VyQGludS5hYy5rciIsImlhdCI6MTczNDYxNzQ2MywiZXhwIjoxNzM0NzAzODYzfQ.P5QZ...
```

**⚠️ 중요:**
- 반드시 `Bearer ` (띄어쓰기 포함!) 다음에 토큰을 붙여넣어야 합니다
- 예: `Bearer <여기에_토큰_붙여넣기>`

### Step 3: Authorize 버튼 클릭

하단의 **"Authorize"** 버튼 클릭

### Step 4: 완료

🔒 아이콘이 **🔓**으로 변경됩니다.

**"Close"** 버튼으로 창 닫기

**✅ 이제 모든 API를 사용할 수 있습니다!**

---

## 📚 5. 주요 API 사용 예시

### 5-1. 토큰 갱신

**POST /api/auth/refresh**

```json
{
  "refreshToken": "여기에_리프레시_토큰_붙여넣기"
}
```

---

## 💡 6. Swagger UI 주요 기능

### 📖 API 문서 자동 생성

각 API를 클릭하면:

- **Parameters**: 요청 파라미터 정보
- **Request body**: 요청 데이터 형식
- **Responses**: 가능한 응답 코드와 예시
- **Example Value**: 샘플 데이터

### 🎨 응답 코드 색상

- **🟢 200-299**: 성공
- **🟡 400-499**: 클라이언트 오류 (잘못된 요청)
- **🔴 500-599**: 서버 오류

### 📋 Schema 보기

**Schemas** 섹션 (페이지 하단):
- 모든 데이터 구조 확인 가능
- 각 필드의 타입, 설명, 필수 여부 등

### 💾 요청/응답 저장

**Responses** 섹션에서:
- **Download**: JSON 응답 다운로드
- 복사 아이콘: 응답 복사

---

## 🚨 7. 자주 발생하는 오류

### ❌ 401 Unauthorized

**원인**: JWT 토큰이 없거나 만료됨

**해결:**
1. 다시 로그인하여 새 토큰 받기
2. Authorize 버튼으로 토큰 재등록

### ❌ 400 Bad Request

**원인**: 잘못된 요청 데이터

**해결:**
- 필수 필드가 모두 입력되었는지 확인
- 데이터 형식이 올바른지 확인 (이메일 형식, 비밀번호 길이 등)

### ❌ 409 Conflict

**원인**: 중복된 데이터 (이미 존재하는 이메일이나 학번)

**해결:**
- 다른 이메일/학번으로 시도

### ❌ 500 Internal Server Error

**원인**: 서버 오류

**해결:**
1. 백엔드 로그 확인: `docker-compose logs backend`
2. 데이터베이스 연결 확인
3. 서비스 재시작: `docker-compose restart backend`

### ❓ FCM 토큰을 어떻게 입력하나요?

**답변**: **입력하지 않아도 됩니다!**

FCM 토큰은 푸시 알림을 받기 위한 선택적 필드입니다.

**Swagger 테스트 시:**
```json
{
  "email": "test@inu.ac.kr",
  "password": "password123"
}
```

**Flutter 앱 개발 시:**
```json
{
  "email": "test@inu.ac.kr",
  "password": "password123",
  "fcmToken": "자동_발급받은_토큰"
}
```

**더미 토큰으로 테스트:**
```json
{
  "email": "test@inu.ac.kr",
  "password": "password123",
  "fcmToken": "test-dummy-token-12345"
}
```

---

## 🎯 8. 실전 테스트 시나리오

### 시나리오 1: 새 사용자 등록 및 로그인

```
1. POST /api/auth/signup
   → 회원가입

2. POST /api/auth/login
   → 로그인 (토큰 받기)

3. 🔓 Authorize
   → 토큰 등록

4. 인증이 필요한 다른 API 테스트
```

### 시나리오 2: 기존 사용자 로그인

```
1. POST /api/auth/login
   → 기존 계정으로 로그인

2. 🔓 Authorize
   → 토큰 등록

3. API 테스트
```

---

## 🔧 9. 고급 기능

### cURL 명령어 생성

각 API의 **Execute** 버튼 아래:

**"Curl"** 탭 클릭 → 터미널에서 실행 가능한 명령어 복사

```bash
curl -X 'POST' \
  'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "swagger@inu.ac.kr",
  "password": "password123"
}'
```

### Request URL 확인

**Request URL** 섹션에서 실제 호출되는 URL 확인 가능

---

## 📱 10. 모바일 앱 개발 시 활용

Swagger에서 테스트한 API를 Flutter 앱에서 호출할 때:

**Swagger에서 확인:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@inu.ac.kr",
  "password": "password123"
}
```

**Flutter 코드로 변환:**
```dart
final response = await http.post(
  Uri.parse('http://your-server.com/api/auth/login'),
  headers: {'Content-Type': 'application/json'},
  body: jsonEncode({
    'email': 'test@inu.ac.kr',
    'password': 'password123',
  }),
);
```

---

## 📊 11. API 목록

### 🔐 인증 API (auth-controller)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| POST | /api/auth/signup | 회원가입 | ❌ |
| POST | /api/auth/login | 로그인 | ❌ |
| POST | /api/auth/refresh | 토큰 갱신 | ❌ |

### 📢 공지사항 API (추가 구현 필요)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| GET | /api/notices | 공지사항 목록 | ❌ |
| GET | /api/notices/{id} | 공지사항 상세 | ❌ |
| GET | /api/notices/search | 공지사항 검색 | ❌ |

### 🔖 북마크 API (추가 구현 필요)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|-----------|
| GET | /api/bookmarks | 내 북마크 목록 | ✅ |
| POST | /api/bookmarks | 북마크 추가 | ✅ |
| DELETE | /api/bookmarks/{id} | 북마크 삭제 | ✅ |

---

## 💡 팁 & 트릭

### 1. 빠른 테스트

**브라우저 북마크에 추가:**
- http://localhost:8080/swagger-ui.html

### 2. 여러 계정 테스트

각 계정마다:
1. 로그인
2. 토큰 복사
3. 메모장에 저장
4. 필요할 때 Authorize로 변경

### 3. API 스펙 다운로드

**http://localhost:8080/v3/api-docs** 접속
- JSON 형식의 OpenAPI 스펙 다운로드 가능
- Postman 등 다른 도구로 import 가능

---

## 🎓 학습 자료

- **Swagger 공식 문서**: https://swagger.io/docs/
- **OpenAPI 스펙**: https://spec.openapis.org/oas/latest.html
- **JWT 이해하기**: https://jwt.io/

---

## 📝 요약

### 🚀 빠른 시작

1. **접속**: http://localhost:8080/swagger-ui.html
2. **회원가입**: POST /api/auth/signup
3. **로그인**: POST /api/auth/login
4. **토큰 등록**: 🔓 Authorize → `Bearer <토큰>`
5. **API 테스트**: Try it out → Execute

### ⚡ 핵심 포인트

- **Try it out** 버튼으로 편집 모드 활성화
- **Execute** 버튼으로 실행
- 인증 필요 API는 **🔓 Authorize** 먼저
- 토큰 형식: **`Bearer <토큰>`** (띄어쓰기 주의!)

---

**즐거운 API 테스트 되세요! 🎉**
