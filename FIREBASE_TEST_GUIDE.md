# Firebase Authentication 테스트 가이드

## 🎯 개요

Firebase Authentication으로 마이그레이션한 API를 테스트하는 방법을 안내합니다.

## ✅ 사전 확인

### 1. 서버 상태 확인

```bash
# Docker 컨테이너 상태
docker-compose ps

# 백엔드 로그 확인 (Firebase 초기화 메시지 확인)
docker-compose logs backend | grep -i firebase
# 출력 예시: "Firebase Admin SDK initialized successfully"
```

### 2. Swagger UI 접속

브라우저에서 다음 URL 접속:
```
http://localhost:8080/swagger-ui.html
```

또는:
```
http://localhost:8080/swagger-ui/index.html
```

## 🔥 Firebase Web API Key 확인 방법

Firebase Authentication 테스트를 위해서는 **Web API Key**가 필요합니다.

### 단계별 가이드

1. **Firebase Console 접속**
   - https://console.firebase.google.com/

2. **프로젝트 선택**
   - 프로젝트: `daon-47f9c`

3. **프로젝트 설정 열기**
   - 좌측 상단의 ⚙️ (톱니바퀴) 아이콘 클릭
   - "프로젝트 설정" 선택

4. **Web API Key 복사**
   - "일반" 탭 선택
   - "웹 API 키" 항목 찾기
   - 키 복사 (예: `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`)

## 📋 테스트 방법

### 방법 1: 자동화 스크립트 사용 (추천)

```bash
# Firebase Authentication 테스트
./test_firebase_simple.sh
```

스크립트 실행 시:
1. Firebase Web API Key 입력 요청
2. 자동으로 테스트 계정 생성
3. Firebase ID Token 발급
4. 서버 로그인 API 테스트
5. 인증이 필요한 API 테스트

#### Web API Key 없이 실행 (레거시 API만 테스트)

```bash
# Enter만 입력하면 레거시 회원가입 API 테스트
./test_firebase_simple.sh
# Web API Key 입력 프롬프트에서 그냥 Enter
```

### 방법 2: Python 스크립트 사용

```bash
# Firebase Admin SDK 사용
python3 test_firebase_auth.py
```

더 상세한 테스트가 필요한 경우 사용합니다.

### 방법 3: Swagger UI 사용 (수동 테스트)

#### A. 레거시 회원가입 API 테스트

1. Swagger UI 접속: http://localhost:8080/swagger-ui.html
2. **POST /api/auth/signup** 엔드포인트 찾기
3. "Try it out" 클릭
4. 요청 본문 입력:

```json
{
  "studentId": "202199888",
  "email": "swaggertest99888@inu.ac.kr",
  "password": "testpassword123",
  "name": "Swagger테스트"
}
```

5. "Execute" 클릭
6. 응답 확인

#### B. Firebase Authentication 테스트

Firebase Authentication을 Swagger UI에서 직접 테스트하려면:

1. **Firebase에서 직접 회원가입**
   - Firebase Console → Authentication → Users 탭
   - "사용자 추가" 클릭
   - 이메일, 비밀번호 입력

2. **Firebase ID Token 발급**

   방법 A: REST API 사용
   ```bash
   curl -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_WEB_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@inu.ac.kr",
       "password": "your-password",
       "returnSecureToken": true
     }'
   ```

   방법 B: 브라우저 콘솔 (Firebase SDK 설치 필요)
   ```javascript
   // Firebase 초기화 후
   firebase.auth().signInWithEmailAndPassword('test@inu.ac.kr', 'password')
     .then(userCredential => userCredential.user.getIdToken())
     .then(token => console.log('ID Token:', token));
   ```

3. **Swagger UI에서 로그인 API 테스트**
   - **POST /api/auth/login** 엔드포인트 찾기
   - "Try it out" 클릭
   - 요청 본문에 발급받은 ID Token 입력:

   ```json
   {
     "idToken": "발급받은_ID_Token_여기에_붙여넣기",
     "fcmToken": "test-fcm-token"
   }
   ```

   - "Execute" 클릭
   - 응답 확인

4. **인증이 필요한 API 테스트**
   - 북마크 조회 등 인증이 필요한 API 선택
   - Swagger UI 상단의 "Authorize" 버튼 클릭
   - `Bearer <ID_Token>` 형식으로 입력
   - "Authorize" 클릭
   - API 테스트 실행

## 🧪 테스트 시나리오

### 시나리오 1: 전체 플로우 테스트

```bash
# 1. 서버 상태 확인
docker-compose ps

# 2. Firebase 초기화 확인
docker-compose logs backend | grep -i "Firebase Admin SDK initialized"

# 3. 자동화 테스트 실행
./test_firebase_simple.sh
# Web API Key 입력 시 전체 테스트 진행
```

### 시나리오 2: 레거시 API만 테스트

```bash
# 레거시 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202199999",
    "email": "test99999@inu.ac.kr",
    "password": "testpassword123",
    "name": "테스트사용자"
  }' | python3 -m json.tool
```

### 시나리오 3: Firebase Authentication 플로우

```bash
# 1. Firebase에서 ID Token 발급
WEB_API_KEY="your-web-api-key"
ID_TOKEN=$(curl -s -X POST \
  "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$WEB_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123",
    "returnSecureToken": true
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['idToken'])")

# 2. 서버 로그인 API 호출
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"idToken\": \"$ID_TOKEN\",
    \"fcmToken\": \"test-fcm-token\"
  }" | python3 -m json.tool

# 3. 인증이 필요한 API 호출
curl -X GET http://localhost:8080/api/bookmarks \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "Content-Type: application/json" | python3 -m json.tool
```

## 📊 예상 결과

### 성공 케이스

#### 회원가입 API
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다",
  "data": {
    "id": 25,
    "studentId": "202199999",
    "email": "test@inu.ac.kr",
    "name": "테스트사용자",
    "role": "USER"
  }
}
```

#### Firebase 로그인 API
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 26,
      "studentId": "firebase-uid-12345",
      "email": "test@inu.ac.kr",
      "name": "테스트사용자",
      "role": "USER"
    }
  }
}
```

### 실패 케이스

#### 인증 실패 (401 Unauthorized)
```json
{
  "success": false,
  "message": "인증에 실패했습니다: Invalid ID token",
  "timestamp": "2025-11-19T17:30:00"
}
```

## 🔍 문제 해결

### 1. Firebase 초기화 실패

**증상:**
```
ERROR c.i.n.c.FirebaseConfig : Failed to initialize Firebase Admin SDK
```

**해결:**
```bash
# firebase-credentials.json 파일 확인
ls -la firebase-credentials.json

# .env 파일 확인
cat .env | grep FCM_CREDENTIALS_PATH

# 서버 재시작
docker-compose restart backend
```

### 2. ID Token 발급 실패

**증상:**
```json
{
  "error": {
    "code": 400,
    "message": "INVALID_EMAIL"
  }
}
```

**해결:**
- Firebase Console에서 이메일/비밀번호 인증이 활성화되었는지 확인
- Authentication → Sign-in method → 이메일/비밀번호 활성화

### 3. 서버 로그인 실패

**증상:**
```json
{
  "success": false,
  "message": "인증에 실패했습니다: ..."
}
```

**해결:**
```bash
# 서버 로그 확인
docker-compose logs backend --tail=50

# Firebase 토큰이 올바른지 확인
# ID Token은 1시간 후 만료되므로 새로 발급받기
```

### 4. CORS 에러

**증상:**
```
Access-Control-Allow-Origin error
```

**해결:**
- 이미 SecurityConfig에서 CORS 설정이 되어 있음
- 서버 재시작: `docker-compose restart backend`

## 📚 참고 자료

- [Firebase Authentication REST API](https://firebase.google.com/docs/reference/rest/auth)
- [Firebase Admin SDK 문서](https://firebase.google.com/docs/admin/setup)
- [프로젝트 마이그레이션 가이드](./FIREBASE_AUTHENTICATION_MIGRATION.md)
- [Firebase 설정 가이드](./FIREBASE_SETUP.md)

## 💡 클라이언트 통합 가이드

### React Native 예시

```javascript
import auth from '@react-native-firebase/auth';
import axios from 'axios';

// 로그인
const login = async (email, password) => {
  // 1. Firebase로 로그인
  const userCredential = await auth().signInWithEmailAndPassword(email, password);

  // 2. ID Token 발급
  const idToken = await userCredential.user.getIdToken();

  // 3. 서버 로그인 API 호출
  const response = await axios.post('http://localhost:8080/api/auth/login', {
    idToken: idToken,
    fcmToken: 'your-fcm-token'
  });

  return response.data;
};

// API 요청 시 자동으로 토큰 추가
axios.interceptors.request.use(async (config) => {
  const user = auth().currentUser;
  if (user) {
    const token = await user.getIdToken(true); // 자동 갱신
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## ✅ 테스트 체크리스트

- [ ] Firebase Admin SDK 초기화 성공 확인
- [ ] Swagger UI 접속 가능
- [ ] 레거시 회원가입 API 테스트 성공
- [ ] Firebase Web API Key 확인
- [ ] Firebase에서 ID Token 발급 성공
- [ ] 서버 로그인 API 테스트 성공
- [ ] 인증이 필요한 API 테스트 성공
- [ ] 토큰 만료 후 자동 갱신 테스트

---

**작성일**: 2025-11-19
**프로젝트**: Incheon University Notice App
**Firebase 프로젝트**: daon-47f9c
