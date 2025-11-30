# 클라이언트 통합 가이드 (Client Integration Guide)

## 📱 회원가입 & 로그인 플로우

### ✅ 수정된 회원가입 플로우 (권장)

이제 서버에서 Firebase 사용자를 자동으로 생성하므로, 아래 단계를 따라주세요:

#### 1️⃣ 회원가입 API 호출

```javascript
// React Native / Web
const signUp = async (email, password, studentId, name) => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/signup', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: email,           // 예: test@inu.ac.kr
        password: password,     // 예: password123
        studentId: studentId,   // 예: 202112345
        name: name             // 예: 홍길동
      })
    });

    const data = await response.json();

    if (data.success) {
      console.log('✅ 회원가입 성공:', data.data);
      // 다음 단계: Firebase 로그인
      return data.data;
    } else {
      console.error('❌ 회원가입 실패:', data.message);
    }
  } catch (error) {
    console.error('❌ 네트워크 오류:', error);
  }
};
```

**서버에서 처리:**
- ✅ Firebase Authentication에 사용자 생성
- ✅ DB에 사용자 정보 저장 (firebaseUid 포함)
- ✅ 이메일 인증 링크 발송 (선택사항)

---

#### 2️⃣ Firebase 로그인 및 ID Token 발급

회원가입이 성공하면, **클라이언트에서 Firebase로 로그인**하여 ID Token을 발급받아야 합니다:

```javascript
// React Native
import auth from '@react-native-firebase/auth';
import messaging from '@react-native-firebase/messaging';

const loginAndGetTokens = async (email, password) => {
  try {
    // 1. Firebase 로그인
    const userCredential = await auth().signInWithEmailAndPassword(email, password);
    console.log('✅ Firebase 로그인 성공:', userCredential.user.uid);

    // 2. ID Token 발급 (서버 인증용)
    const idToken = await userCredential.user.getIdToken();
    console.log('✅ ID Token 발급 완료:', idToken.substring(0, 20) + '...');

    // 3. FCM Token 발급 (푸시 알림용)
    const fcmToken = await messaging().getToken();
    console.log('✅ FCM Token 발급 완료:', fcmToken.substring(0, 20) + '...');

    return { idToken, fcmToken };
  } catch (error) {
    console.error('❌ Firebase 로그인 실패:', error.message);
    throw error;
  }
};
```

**React Web (Firebase v9+):**
```javascript
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';
import { getToken } from 'firebase/messaging';
import { messaging } from './firebaseConfig';

const loginAndGetTokens = async (email, password) => {
  const auth = getAuth();

  // 1. Firebase 로그인
  const userCredential = await signInWithEmailAndPassword(auth, email, password);

  // 2. ID Token 발급
  const idToken = await userCredential.user.getIdToken();

  // 3. FCM Token 발급
  const fcmToken = await getToken(messaging, {
    vapidKey: 'YOUR_VAPID_KEY'
  });

  return { idToken, fcmToken };
};
```

---

#### 3️⃣ 서버 로그인 API 호출 (토큰 등록)

발급받은 토큰을 서버에 등록합니다:

```javascript
const loginToServer = async (idToken, fcmToken) => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        idToken: idToken,
        fcmToken: fcmToken  // 선택사항, 푸시 알림을 받으려면 필수
      })
    });

    const data = await response.json();

    if (data.success) {
      console.log('✅ 서버 로그인 성공:', data.data);

      // 로컬 스토리지에 토큰 저장
      localStorage.setItem('idToken', data.data.idToken);
      localStorage.setItem('user', JSON.stringify(data.data.user));

      return data.data;
    } else {
      console.error('❌ 서버 로그인 실패:', data.message);
    }
  } catch (error) {
    console.error('❌ 네트워크 오류:', error);
  }
};
```

---

#### 4️⃣ 전체 통합 예시

모든 단계를 하나로 합친 완전한 예시:

```javascript
import auth from '@react-native-firebase/auth';
import messaging from '@react-native-firebase/messaging';

// 🔹 회원가입 전체 플로우
const completeSignUp = async (email, password, studentId, name) => {
  try {
    // STEP 1: 서버에 회원가입 요청
    console.log('📝 Step 1: 서버 회원가입 시작...');
    const signUpResponse = await fetch('http://localhost:8080/api/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, studentId, name })
    });

    const signUpData = await signUpResponse.json();
    if (!signUpData.success) {
      throw new Error(signUpData.message);
    }
    console.log('✅ Step 1 완료: 서버 회원가입 성공');

    // STEP 2: Firebase 로그인
    console.log('🔐 Step 2: Firebase 로그인 시작...');
    const userCredential = await auth().signInWithEmailAndPassword(email, password);
    console.log('✅ Step 2 완료: Firebase 로그인 성공');

    // STEP 3: ID Token 발급
    console.log('🎫 Step 3: ID Token 발급 시작...');
    const idToken = await userCredential.user.getIdToken();
    console.log('✅ Step 3 완료: ID Token 발급 완료');

    // STEP 4: FCM Token 발급
    console.log('📲 Step 4: FCM Token 발급 시작...');
    const fcmToken = await messaging().getToken();
    console.log('✅ Step 4 완료: FCM Token 발급 완료');

    // STEP 5: 서버 로그인 (토큰 등록)
    console.log('🔗 Step 5: 서버 로그인 시작...');
    const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken, fcmToken })
    });

    const loginData = await loginResponse.json();
    if (!loginData.success) {
      throw new Error(loginData.message);
    }
    console.log('✅ Step 5 완료: 서버 로그인 성공');

    // 로컬 스토리지에 저장
    localStorage.setItem('idToken', loginData.data.idToken);
    localStorage.setItem('user', JSON.stringify(loginData.data.user));

    console.log('🎉 회원가입 전체 플로우 완료!');
    return loginData.data;

  } catch (error) {
    console.error('❌ 회원가입 실패:', error.message);
    throw error;
  }
};

// 사용 예시
completeSignUp(
  'test@inu.ac.kr',
  'password123',
  '202112345',
  '홍길동'
)
  .then(userData => {
    console.log('로그인한 사용자:', userData.user);
  })
  .catch(error => {
    console.error('오류:', error);
  });
```

---

## 🔐 기존 사용자 로그인 플로우

이미 가입한 사용자의 로그인:

```javascript
const loginExistingUser = async (email, password) => {
  try {
    // 1. Firebase 로그인
    const userCredential = await auth().signInWithEmailAndPassword(email, password);

    // 2. ID Token 발급
    const idToken = await userCredential.user.getIdToken();

    // 3. FCM Token 발급
    const fcmToken = await messaging().getToken();

    // 4. 서버 로그인
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken, fcmToken })
    });

    const data = await response.json();

    if (data.success) {
      localStorage.setItem('idToken', data.data.idToken);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      return data.data;
    }
  } catch (error) {
    console.error('로그인 실패:', error);
    throw error;
  }
};
```

---

## 🔑 API 요청 시 인증 헤더 추가

로그인 후 다른 API를 호출할 때는 ID Token을 헤더에 포함:

```javascript
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
```

---

## ❓ FAQ

### Q1: idToken은 어디서 발급받나요?
**A:** 클라이언트에서 Firebase SDK로 로그인 후 `user.getIdToken()`으로 발급받습니다. 서버에서는 발급할 수 없습니다.

### Q2: fcmToken은 어디서 발급받나요?
**A:** 클라이언트 디바이스에서 Firebase Messaging SDK로 발급받습니다 (`messaging().getToken()`). 서버에서는 발급할 수 없습니다.

### Q3: 회원가입 후 바로 로그인해야 하나요?
**A:** 네! 회원가입 후 반드시 Firebase 로그인 → 토큰 발급 → 서버 로그인 과정을 거쳐야 idToken과 fcmToken이 등록됩니다.

### Q4: idToken 만료되면 어떻게 하나요?
**A:** Firebase ID Token은 1시간마다 만료됩니다. `user.getIdToken(true)`로 강제 갱신하거나, Firebase SDK가 자동으로 갱신합니다.

### Q5: 이메일 인증은 필수인가요?
**A:** 선택사항입니다. 이메일 인증 전에도 로그인 가능하지만, 보안을 위해 인증을 권장합니다.

---

## 🚨 주의사항

1. **⚠️ idToken과 fcmToken은 서버에서 발급할 수 없습니다**
   - 반드시 클라이언트에서 Firebase SDK로 발급받아야 합니다

2. **⚠️ 회원가입 후 로그인 필수**
   - 회원가입만으로는 토큰이 등록되지 않습니다
   - 반드시 위 플로우대로 진행하세요

3. **⚠️ 토큰 만료 처리**
   - ID Token은 1시간마다 만료됩니다
   - 401 Unauthorized 응답 시 토큰을 갱신하세요

4. **⚠️ FCM 권한 요청**
   - iOS: Info.plist에 권한 추가 필요
   - Android: AndroidManifest.xml에 권한 추가 필요

---

## 📚 참고 문서

- [Firebase Authentication 문서](https://firebase.google.com/docs/auth)
- [Firebase Cloud Messaging 문서](https://firebase.google.com/docs/cloud-messaging)
- [React Native Firebase](https://rnfirebase.io/)
