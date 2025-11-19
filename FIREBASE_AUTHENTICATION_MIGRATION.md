# Firebase Authentication 전환 가이드

> JWT 기반 인증에서 Firebase Authentication으로 전환되었습니다.

## 📋 변경 사항 요약

### 주요 변경점
- **JWT → Firebase ID Token 검증으로 변경**
- **인증 필터 교체**: `JwtAuthenticationFilter` → `FirebaseAuthenticationFilter`
- **로그인 방식 변경**: 이메일/비밀번호 → Firebase ID Token
- **토큰 갱신 제거**: Firebase SDK가 자동 처리

### 삭제된 API
- ~~`POST /api/auth/refresh`~~ - Firebase SDK가 자동으로 토큰 갱신을 처리합니다

### 변경된 API

#### POST /api/auth/login

**Before (JWT):**
```json
{
  "email": "user@inu.ac.kr",
  "password": "password123",
  "fcmToken": "optional-fcm-token"
}
```

**After (Firebase):**
```json
{
  "idToken": "firebase-id-token-here",
  "fcmToken": "optional-fcm-token"
}
```

**Response:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "idToken": "firebase-id-token",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@inu.ac.kr",
      "name": "사용자",
      "studentId": "2021000000",
      "role": "USER"
    }
  }
}
```

## 🔧 서버 설정

### 1. Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 생성 또는 기존 프로젝트 선택
3. **프로젝트 설정** → **서비스 계정** 탭
4. **새 비공개 키 생성** 클릭
5. `firebase-credentials.json` 다운로드
6. 프로젝트 루트 디렉토리에 배치

### 2. Firebase Authentication 활성화

Firebase Console → **Authentication** → **Sign-in method**

활성화할 로그인 방법:
- ✅ 이메일/비밀번호
- ✅ Google (선택)
- ✅ 기타 소셜 로그인 (선택)

### 3. 환경 변수 설정

`.env` 파일 또는 환경 변수:
```bash
FCM_CREDENTIALS_PATH=./firebase-credentials.json
```

### 4. .gitignore 확인

```gitignore
# Firebase 서비스 계정 키 (절대 커밋하지 마세요!)
firebase-credentials.json
firebase-service-account.json
*-firebase-adminsdk-*.json
```

## 📱 클라이언트 구현

### React Native 예시

#### 1. Firebase SDK 설치

```bash
npm install @react-native-firebase/app @react-native-firebase/auth
# 또는
yarn add @react-native-firebase/app @react-native-firebase/auth
```

#### 2. Firebase 초기화

```javascript
// firebase.config.js
import auth from '@react-native-firebase/auth';

export const firebaseAuth = auth();
```

#### 3. 회원가입

```javascript
import auth from '@react-native-firebase/auth';

async function signUp(email, password, name) {
  try {
    // Firebase에 회원가입
    const userCredential = await auth().createUserWithEmailAndPassword(email, password);

    // 프로필 업데이트
    await userCredential.user.updateProfile({
      displayName: name
    });

    // 이메일 인증 메일 발송
    await userCredential.user.sendEmailVerification();

    console.log('회원가입 성공! 이메일 인증을 확인해주세요.');
    return userCredential.user;
  } catch (error) {
    console.error('회원가입 실패:', error.message);
    throw error;
  }
}
```

#### 4. 로그인

```javascript
import auth from '@react-native-firebase/auth';
import axios from 'axios';

async function login(email, password) {
  try {
    // 1. Firebase로 로그인
    const userCredential = await auth().signInWithEmailAndPassword(email, password);

    // 2. Firebase ID Token 가져오기
    const idToken = await userCredential.user.getIdToken();

    // 3. 서버에 ID Token 전송하여 사용자 정보 동기화
    const response = await axios.post('http://localhost:8080/api/auth/login', {
      idToken: idToken,
      fcmToken: 'fcm-token-here' // FCM 토큰 (선택사항)
    });

    console.log('로그인 성공:', response.data);
    return response.data;
  } catch (error) {
    console.error('로그인 실패:', error.message);
    throw error;
  }
}
```

#### 5. 토큰 자동 갱신 및 API 요청

```javascript
import auth from '@react-native-firebase/auth';
import axios from 'axios';

// Axios 인터셉터 설정 (모든 API 요청에 자동으로 토큰 추가)
axios.interceptors.request.use(
  async (config) => {
    const currentUser = auth().currentUser;

    if (currentUser) {
      // getIdToken(true)는 토큰이 만료되었을 경우 자동으로 갱신합니다
      const idToken = await currentUser.getIdToken(true);
      config.headers.Authorization = `Bearer ${idToken}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// API 요청 예시
async function fetchNotices() {
  try {
    const response = await axios.get('http://localhost:8080/api/notices');
    return response.data;
  } catch (error) {
    console.error('공지사항 조회 실패:', error);
    throw error;
  }
}

async function createBookmark(noticeId) {
  try {
    const response = await axios.post('http://localhost:8080/api/bookmarks', {
      noticeId: noticeId
    });
    return response.data;
  } catch (error) {
    console.error('북마크 생성 실패:', error);
    throw error;
  }
}
```

#### 6. 로그아웃

```javascript
import auth from '@react-native-firebase/auth';

async function logout() {
  try {
    await auth().signOut();
    console.log('로그아웃 성공');
  } catch (error) {
    console.error('로그아웃 실패:', error);
  }
}
```

#### 7. 비밀번호 재설정

```javascript
import auth from '@react-native-firebase/auth';

async function resetPassword(email) {
  try {
    await auth().sendPasswordResetEmail(email);
    console.log('비밀번호 재설정 이메일이 발송되었습니다.');
  } catch (error) {
    console.error('비밀번호 재설정 실패:', error);
  }
}
```

### React 웹 예시

#### 1. Firebase SDK 설치

```bash
npm install firebase
# 또는
yarn add firebase
```

#### 2. Firebase 초기화

```javascript
// firebase.config.js
import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_AUTH_DOMAIN",
  projectId: "YOUR_PROJECT_ID",
  // ... 기타 설정
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
```

#### 3. 로그인 구현

```javascript
import { signInWithEmailAndPassword, getIdToken } from 'firebase/auth';
import { auth } from './firebase.config';
import axios from 'axios';

async function login(email, password) {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const idToken = await getIdToken(userCredential.user);

    const response = await axios.post('/api/auth/login', {
      idToken: idToken
    });

    return response.data;
  } catch (error) {
    console.error('로그인 실패:', error);
    throw error;
  }
}
```

## 🔄 인증 플로우 비교

### 이전 (JWT 방식)

```
1. 클라이언트: 이메일 + 비밀번호 → 서버
2. 서버: DB에서 비밀번호 검증
3. 서버: JWT Access Token + Refresh Token 생성
4. 클라이언트: 토큰 저장
5. API 요청 시: Authorization: Bearer {accessToken}
6. Access Token 만료 시: Refresh Token으로 갱신 요청
```

### 현재 (Firebase 방식)

```
1. 클라이언트: Firebase SDK로 로그인 (이메일 + 비밀번호)
2. Firebase: 인증 처리
3. Firebase: ID Token 발급 (1시간 유효)
4. 클라이언트: ID Token → 서버
5. 서버: Firebase Admin SDK로 ID Token 검증
6. 서버: 사용자 정보 확인/생성
7. API 요청 시: Authorization: Bearer {idToken}
8. ID Token 만료 시: Firebase SDK가 자동 갱신 (getIdToken(true))
```

## 📊 주요 차이점

| 항목 | JWT | Firebase Authentication |
|------|-----|-------------------------|
| **토큰 발급** | 서버 | Firebase |
| **토큰 검증** | 서버 (시크릿 키) | Firebase Admin SDK |
| **토큰 갱신** | 수동 (/refresh 엔드포인트) | 자동 (Firebase SDK) |
| **유효기간** | 커스텀 설정 (24시간) | 1시간 (자동 갱신) |
| **비밀번호 관리** | 서버 DB | Firebase |
| **이메일 인증** | 직접 구현 필요 | Firebase 제공 |
| **소셜 로그인** | 직접 구현 필요 | Firebase 제공 (Google, Facebook 등) |
| **보안** | 서버에서 직접 관리 | Google 인프라 보안 |

## 🎯 장점

### Firebase Authentication 사용의 이점

1. **자동 토큰 갱신**: 클라이언트 SDK가 자동으로 처리
2. **소셜 로그인 간편화**: Google, Facebook, Apple 등 쉽게 추가
3. **이메일 인증 자동화**: Firebase가 이메일 발송 처리
4. **비밀번호 재설정**: Firebase가 처리
5. **보안 강화**: Google 인프라의 보안 기능 활용
6. **멀티플랫폼 지원**: iOS, Android, Web 통합 SDK
7. **서버 부담 감소**: 인증 로직을 Firebase에 위임

## ⚠️ 주의사항

1. **회원가입 API**: 서버의 `/api/auth/signup`도 사용 가능하지만, **Firebase SDK 사용을 권장**합니다
2. **토큰 갱신**: `/api/auth/refresh` API는 **삭제되었습니다**
3. **Firebase 설정 필수**: 클라이언트에서 반드시 Firebase SDK를 설정해야 합니다
4. **서비스 계정 키 보안**: `firebase-credentials.json`은 **절대 Git에 커밋하지 마세요**
5. **자동 회원가입**: Firebase로 로그인 시 서버 DB에 사용자가 없으면 자동으로 생성됩니다

## 🔐 보안 권장사항

1. **서비스 계정 키 관리**
   ```bash
   # 프로덕션 환경
   export FCM_CREDENTIALS_PATH=/secure/path/firebase-credentials.json

   # 개발 환경
   export FCM_CREDENTIALS_PATH=./firebase-credentials.json
   ```

2. **Firebase Security Rules 설정**
   - Firestore, Storage 등 사용 시 적절한 보안 규칙 설정

3. **환경별 Firebase 프로젝트 분리**
   - 개발: firebase-dev
   - 스테이징: firebase-staging
   - 프로덕션: firebase-prod

## 📚 추가 자료

- [Firebase Authentication 공식 문서](https://firebase.google.com/docs/auth)
- [React Native Firebase 문서](https://rnfirebase.io/)
- [Firebase Admin SDK 문서](https://firebase.google.com/docs/admin/setup)
- [Firebase 보안 가이드](https://firebase.google.com/docs/rules)

## 🆘 문제 해결

### 1. "Failed to initialize Firebase Admin SDK" 오류

**원인**: `firebase-credentials.json` 파일을 찾을 수 없거나 잘못된 경로

**해결**:
```bash
# 파일 존재 확인
ls -la firebase-credentials.json

# 경로 확인
export FCM_CREDENTIALS_PATH=./firebase-credentials.json
```

### 2. "Invalid ID token" 오류

**원인**: 만료되었거나 잘못된 ID Token

**해결**:
```javascript
// 토큰 강제 갱신
const idToken = await currentUser.getIdToken(true);
```

### 3. 클라이언트에서 로그인은 되지만 서버 API 호출 시 401 에러

**원인**: ID Token을 서버에 전송하지 않았거나 헤더 형식 오류

**해결**:
```javascript
// 올바른 헤더 형식
headers: {
  'Authorization': `Bearer ${idToken}`
}
```

## 🚀 마이그레이션 체크리스트

- [ ] Firebase 프로젝트 생성
- [ ] Firebase Authentication 활성화
- [ ] 서비스 계정 키 다운로드 및 배치
- [ ] 클라이언트에 Firebase SDK 설치
- [ ] 로그인 로직을 Firebase Authentication으로 변경
- [ ] 토큰 갱신 로직 제거 (자동 갱신으로 대체)
- [ ] API 요청 시 Firebase ID Token 사용
- [ ] 기존 JWT 관련 코드 정리
- [ ] 테스트 및 검증

---

**마이그레이션 완료일**: 2025-11-19
**버전**: 1.0.0
