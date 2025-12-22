# 간단 로그인 가이드

## ✅ 해결 완료!

회원가입 시 Firebase에 사용자가 생성되지 않고, idToken과 fcmToken이 발급되지 않는 문제를 해결했습니다.

---

## 🎯 새로운 간편 로그인 방식

이제 **이메일/비밀번호만으로** 간단하게 로그인할 수 있습니다!

### 1️⃣ 회원가입

```bash
POST /api/auth/signup
Content-Type: application/json

{
  "email": "test@inu.ac.kr",
  "password": "password123",
  "studentId": "202112345",
  "name": "홍길동"
}
```

**서버에서 자동 처리:**
- ✅ Firebase Authentication에 사용자 생성
- ✅ DB에 사용자 저장
- ✅ 이메일 인증 링크 발송

---

### 2️⃣ 로그인 (간편!)

```bash
POST /api/auth/login/email
Content-Type: application/json

{
  "email": "test@inu.ac.kr",
  "password": "password123",
  "fcmToken": "optional_fcm_token"  # 선택사항
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "idToken": "eyJhbGc...",  // Firebase 커스텀 토큰
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "test@inu.ac.kr",
      "name": "홍길동",
      "studentId": "202112345",
      "role": "USER"
    }
  }
}
```

---

### 3️⃣ API 요청 시 토큰 사용

```bash
GET /api/notices
Authorization: Bearer {idToken}
```

---

## 📱 클라이언트 코드 예시

### JavaScript/React Native

```javascript
// 1. 회원가입
const signUp = async (email, password, studentId, name) => {
  const response = await fetch('http://localhost:8080/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, studentId, name })
  });

  const data = await response.json();
  if (data.success) {
    console.log('✅ 회원가입 성공:', data.data);
    return data.data;
  } else {
    throw new Error(data.message);
  }
};

// 2. 로그인
const login = async (email, password) => {
  const response = await fetch('http://localhost:8080/api/auth/login/email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  const data = await response.json();
  if (data.success) {
    // 토큰 저장
    localStorage.setItem('idToken', data.data.idToken);
    localStorage.setItem('user', JSON.stringify(data.data.user));

    console.log('✅ 로그인 성공:', data.data.user);
    return data.data;
  } else {
    throw new Error(data.message);
  }
};

// 3. API 요청
const fetchNotices = async () => {
  const idToken = localStorage.getItem('idToken');

  const response = await fetch('http://localhost:8080/api/notices', {
    headers: {
      'Authorization': `Bearer ${idToken}`,
      'Content-Type': 'application/json'
    }
  });

  return await response.json();
};

// 사용 예시
async function main() {
  try {
    // 회원가입
    await signUp('test@inu.ac.kr', 'password123', '202112345', '홍길동');

    // 로그인
    const loginData = await login('test@inu.ac.kr', 'password123');
    console.log('로그인한 사용자:', loginData.user);

    // API 호출
    const notices = await fetchNotices();
    console.log('공지사항:', notices);

  } catch (error) {
    console.error('오류:', error.message);
  }
}
```

---

## 🔄 전체 플로우

```
1. 회원가입 → POST /api/auth/signup
   ↓
   서버: Firebase 사용자 생성 + DB 저장
   ↓
2. 로그인 → POST /api/auth/login/email
   ↓
   서버: 이메일/비밀번호 검증 + Firebase 커스텀 토큰 발급
   ↓
3. API 요청 → GET /api/notices
   Header: Authorization: Bearer {idToken}
   ↓
   서버: 토큰 검증 + 데이터 반환
```

---

## 🚀 테스트 방법

### 방법 1: curl로 테스트

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123",
    "studentId": "202112345",
    "name": "홍길동"
  }'

# 2. 로그인
curl -X POST http://localhost:8080/api/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123"
  }'

# 3. 공지사항 조회 (토큰 사용)
curl -X GET http://localhost:8080/api/notices \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 방법 2: 자동화 스크립트

```bash
# 서버를 재시작한 후:
./test_complete_auth_flow.sh
```

이 스크립트는:
- ✅ 회원가입
- ✅ 로그인
- ✅ API 요청
- ✅ 결과 확인

을 자동으로 테스트합니다.

---

## ⚠️ 중요: 서버 재시작 필요

변경사항을 적용하려면 **서버를 재시작**해야 합니다:

```bash
# 서버 중지 (Ctrl+C)
# 그 다음:
./gradlew bootRun
```

또는

```bash
# IntelliJ에서 서버 재시작
```

---

## 📊 API 엔드포인트 요약

| 엔드포인트 | 메서드 | 설명 | 인증 필요 |
|-----------|--------|------|----------|
| `/api/auth/signup` | POST | 회원가입 | ❌ |
| `/api/auth/login/email` | POST | **이메일 로그인 (간편!)** | ❌ |
| `/api/auth/login` | POST | Firebase ID Token 로그인 | ❌ |
| `/api/notices` | GET | 공지사항 목록 | ✅ (선택) |
| `/api/notices/{id}` | GET | 공지사항 상세 | ✅ (선택) |

---

## 🎉 차이점

### 이전 방식 (복잡함):
1. 서버 회원가입
2. **클라이언트에서 Firebase SDK 로그인**
3. **Firebase ID Token 발급**
4. **서버 로그인 API 호출**
5. API 사용

### 새로운 방식 (간편!):
1. 서버 회원가입
2. **이메일/비밀번호 로그인** ← 한 번에 끝!
3. API 사용

---

## ❓ FAQ

### Q: Firebase SDK를 사용해야 하나요?
**A:** 아니요! 이제 이메일/비밀번호만으로 로그인 가능합니다.

### Q: 기존 Firebase ID Token 로그인도 사용 가능한가요?
**A:** 네! `/api/auth/login` 엔드포인트는 여전히 사용 가능합니다.

### Q: fcmToken은 필수인가요?
**A:** 선택사항입니다. 푸시 알림을 받으려면 제공하세요.

### Q: 토큰이 만료되면?
**A:** 다시 로그인하세요 (`POST /api/auth/login/email`).

### Q: 이메일 인증은 필수인가요?
**A:** 선택사항입니다. 인증 전에도 로그인 가능합니다.

---

## 🔍 Firebase Console 확인

회원가입 후 Firebase Console에서 확인:

1. https://console.firebase.google.com/ 접속
2. Authentication > Users 탭
3. 새로 생성된 사용자 확인

---

## 📚 관련 문서

- `CLIENT_INTEGRATION_GUIDE.md` - 클라이언트 통합 가이드 (상세)
- `test_complete_auth_flow.sh` - 자동화 테스트 스크립트
- Swagger UI: http://localhost:8080/swagger-ui.html
