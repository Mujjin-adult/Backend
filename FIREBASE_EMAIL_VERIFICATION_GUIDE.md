# Firebase 이메일 인증 가이드

Firebase Authentication을 사용한 이메일 인증 구현 및 설정 가이드입니다.

## 목차

1. [개요](#개요)
2. [클라이언트 측 구현 (권장)](#클라이언트-측-구현-권장)
3. [서버 측 구현](#서버-측-구현)
4. [SMTP 설정 (서버 측 이메일 발송용)](#smtp-설정-서버-측-이메일-발송용)
5. [Firebase Console 설정](#firebase-console-설정)
6. [테스트](#테스트)

---

## 개요

Firebase Authentication은 두 가지 이메일 인증 방법을 제공합니다:

### 1. **클라이언트 측 (권장) ⭐**
- Firebase SDK의 `sendEmailVerification()` 사용
- 구현이 간단하고 빠름
- Firebase에서 이메일 발송 처리
- 추가 SMTP 설정 불필요

### 2. **서버 측**
- Firebase Admin SDK로 인증 링크 생성
- 커스텀 이메일 템플릿 사용 가능
- 서버에서 완전한 이메일 발송 제어
- SMTP 서버 설정 필요

---

## 클라이언트 측 구현 (권장)

### React Native

```javascript
import auth from '@react-native-firebase/auth';

// 회원가입 후 이메일 인증 메일 발송
async function signUpAndSendVerification(email, password) {
  try {
    // 1. Firebase로 회원가입
    const userCredential = await auth().createUserWithEmailAndPassword(email, password);
    const user = userCredential.user;

    // 2. 이메일 인증 메일 발송
    await user.sendEmailVerification();

    console.log('이메일 인증 메일이 발송되었습니다');

    // 3. 서버에 로그인하여 사용자 정보 동기화
    const idToken = await user.getIdToken();
    await loginToServer(idToken);

    return user;
  } catch (error) {
    console.error('회원가입 실패:', error);
    throw error;
  }
}

// 이메일 인증 메일 재발송
async function resendVerificationEmail() {
  try {
    const user = auth().currentUser;

    if (!user) {
      throw new Error('로그인이 필요합니다');
    }

    if (user.emailVerified) {
      console.log('이미 인증된 이메일입니다');
      return;
    }

    await user.sendEmailVerification();
    console.log('이메일 인증 메일이 재발송되었습니다');
  } catch (error) {
    console.error('이메일 재발송 실패:', error);
    throw error;
  }
}

// 이메일 인증 상태 확인
async function checkEmailVerified() {
  const user = auth().currentUser;

  if (!user) {
    return false;
  }

  // 최신 상태로 새로고침
  await user.reload();

  return user.emailVerified;
}
```

### React Web

```javascript
import { getAuth, sendEmailVerification, createUserWithEmailAndPassword } from 'firebase/auth';

const auth = getAuth();

// 회원가입 후 이메일 인증 메일 발송
async function signUpAndSendVerification(email, password) {
  try {
    // 1. Firebase로 회원가입
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    const user = userCredential.user;

    // 2. 이메일 인증 메일 발송
    await sendEmailVerification(user);

    console.log('이메일 인증 메일이 발송되었습니다');

    // 3. 서버에 로그인하여 사용자 정보 동기화
    const idToken = await user.getIdToken();
    await loginToServer(idToken);

    return user;
  } catch (error) {
    console.error('회원가입 실패:', error);
    throw error;
  }
}

// 이메일 인증 메일 재발송
async function resendVerificationEmail() {
  const user = auth.currentUser;

  if (!user) {
    throw new Error('로그인이 필요합니다');
  }

  if (user.emailVerified) {
    console.log('이미 인증된 이메일입니다');
    return;
  }

  await sendEmailVerification(user);
  console.log('이메일 인증 메일이 재발송되었습니다');
}

// 이메일 인증 상태 확인
async function checkEmailVerified() {
  const user = auth.currentUser;

  if (!user) {
    return false;
  }

  // 최신 상태로 새로고침
  await user.reload();

  return user.emailVerified;
}
```

### 커스텀 이메일 템플릿 (클라이언트)

```javascript
// ActionCodeSettings로 커스텀 설정
const actionCodeSettings = {
  url: 'https://yourapp.com/finishSignUp?cartId=1234',
  handleCodeInApp: true,
  iOS: {
    bundleId: 'com.example.ios'
  },
  android: {
    packageName: 'com.example.android',
    installApp: true,
    minimumVersion: '12'
  },
  dynamicLinkDomain: 'example.page.link'
};

await sendEmailVerification(user, actionCodeSettings);
```

---

## 서버 측 구현

서버에서 Firebase Admin SDK를 사용하여 이메일 인증 링크를 생성하고 발송합니다.

### API 엔드포인트

#### 1. 이메일 인증 메일 발송

```http
POST /api/auth/send-verification-email?email={email}
```

**Request:**
```bash
curl -X POST "http://localhost:8080/api/auth/send-verification-email?email=test@inu.ac.kr"
```

**Response:**
```json
{
  "success": true,
  "message": "이메일 인증 링크가 발송되었습니다",
  "data": null,
  "timestamp": "2025-11-25T15:32:38.087042136"
}
```

#### 2. 이메일 인증 메일 재발송

```http
POST /api/auth/resend-verification-email?email={email}
```

**Request:**
```bash
curl -X POST "http://localhost:8080/api/auth/resend-verification-email?email=test@inu.ac.kr"
```

**Response:**
```json
{
  "success": true,
  "message": "이메일 인증 링크가 발송되었습니다",
  "data": null,
  "timestamp": "2025-11-25T15:33:42.123456789"
}
```

### 서버 코드 구조

#### AuthService.java

```java
public String sendEmailVerification(String email) {
    try {
        // 1. Firebase에서 사용자 조회
        var firebaseUser = FirebaseAuth.getInstance().getUserByEmail(email);

        // 2. 이미 인증된 경우
        if (firebaseUser.isEmailVerified()) {
            return "이미 인증된 이메일입니다";
        }

        // 3. ActionCodeSettings 생성
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
            .setUrl(frontendUrl + "/email-verified")
            .setHandleCodeInApp(false)
            .build();

        // 4. 이메일 인증 링크 생성
        String verificationLink = FirebaseAuth.getInstance()
            .generateEmailVerificationLink(email, actionCodeSettings);

        // 5. 이메일 발송
        emailService.sendFirebaseVerificationEmail(email, verificationLink);

        return "이메일 인증 링크가 발송되었습니다";
    } catch (FirebaseAuthException e) {
        throw new BusinessException("이메일 인증 링크 생성에 실패했습니다: " + e.getMessage());
    }
}
```

#### EmailService.java

```java
public void sendFirebaseVerificationEmail(String toEmail, String verificationLink) {
    String subject = "[인천대 공지사항] 이메일 인증";

    String content = String.format(
        "인천대학교 공지사항 앱 회원가입을 환영합니다!\n\n" +
        "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n" +
        "%s\n\n" +
        "이메일 인증을 완료하면 모든 기능을 이용하실 수 있습니다.\n\n" +
        "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.",
        verificationLink
    );

    sendEmail(toEmail, subject, content);
}
```

---

## SMTP 설정 (서버 측 이메일 발송용)

서버에서 이메일을 발송하려면 SMTP 서버 설정이 필요합니다.

### application.yml 설정

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # Gmail 앱 비밀번호
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

app:
  frontend:
    url: http://localhost:3000  # 프론트엔드 URL
```

### Gmail 앱 비밀번호 생성

1. Gmail 계정에 로그인
2. Google 계정 설정 → 보안
3. "2단계 인증" 활성화
4. "앱 비밀번호" 생성
5. 생성된 16자리 비밀번호를 `application.yml`에 입력

### 환경 변수 사용 (권장)

보안을 위해 환경 변수를 사용하세요:

```yaml
spring:
  mail:
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

```bash
# docker-compose.yml
environment:
  MAIL_USERNAME: your-email@gmail.com
  MAIL_PASSWORD: your-app-password
```

---

## Firebase Console 설정

### 1. 이메일 템플릿 커스터마이징

Firebase Console에서 이메일 템플릿을 커스터마이징할 수 있습니다:

1. **Firebase Console 접속**: https://console.firebase.google.com/
2. **Authentication → Templates** 메뉴로 이동
3. **이메일 주소 확인** 템플릿 선택
4. 템플릿 편집:
   - 발신자 이름 설정
   - 제목 커스터마이징
   - 본문 HTML 편집
   - 언어별 템플릿 설정

### 2. 이메일 템플릿 예시

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>이메일 인증</title>
</head>
<body>
  <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
    <h1>인천대학교 공지사항 앱</h1>
    <p>안녕하세요, %DISPLAY_NAME%님!</p>
    <p>회원가입을 환영합니다. 아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>
    <div style="text-align: center; margin: 30px 0;">
      <a href="%LINK%"
         style="background-color: #4285f4; color: white; padding: 12px 24px;
                text-decoration: none; border-radius: 4px; display: inline-block;">
        이메일 인증하기
      </a>
    </div>
    <p>또는 다음 링크를 복사하여 브라우저에 붙여넣으세요:</p>
    <p style="word-break: break-all;">%LINK%</p>
    <p>감사합니다.</p>
  </div>
</body>
</html>
```

### 3. 도메인 설정

Firebase 이메일 링크가 작동하려면 승인된 도메인을 설정해야 합니다:

1. **Authentication → Settings → Authorized domains**
2. 도메인 추가:
   - `localhost` (개발용)
   - `yourapp.com` (프로덕션)

---

## 테스트

### Python 테스트 스크립트 실행

```bash
python3 test_firebase_email_verification.py
```

### 테스트 시나리오

1. ✅ Firebase 사용자 생성 (이메일 인증되지 않은 상태)
2. ✅ 서버 로그인 (자동 회원가입)
3. 🔄 이메일 인증 메일 발송
4. 🔄 이메일 인증 메일 재발송

### 수동 테스트

#### 1. Swagger UI에서 테스트

1. http://localhost:8080/swagger-ui/index.html 접속
2. **인증 및 회원관리** 섹션 → **이메일 인증 메일 발송** API 선택
3. 테스트 이메일 입력 (예: `test@inu.ac.kr`)
4. **Execute** 버튼 클릭

#### 2. curl로 테스트

```bash
# 이메일 인증 메일 발송
curl -X POST "http://localhost:8080/api/auth/send-verification-email?email=test@inu.ac.kr"

# 이메일 인증 메일 재발송
curl -X POST "http://localhost:8080/api/auth/resend-verification-email?email=test@inu.ac.kr"
```

#### 3. 클라이언트에서 테스트

```javascript
// React Native/Web
import auth from '@react-native-firebase/auth';

async function testEmailVerification() {
  try {
    // 1. 회원가입
    const userCredential = await auth().createUserWithEmailAndPassword(
      'test@inu.ac.kr',
      'testpass123'
    );

    // 2. 이메일 인증 메일 발송
    await userCredential.user.sendEmailVerification();

    console.log('✅ 이메일 인증 메일이 발송되었습니다');

    // 3. 이메일 확인 후 상태 새로고침
    await userCredential.user.reload();

    if (userCredential.user.emailVerified) {
      console.log('✅ 이메일 인증 완료!');
    } else {
      console.log('⏳ 이메일 인증 대기 중...');
    }
  } catch (error) {
    console.error('❌ 테스트 실패:', error);
  }
}
```

---

## 트러블슈팅

### 1. SMTP 인증 실패

**에러:**
```
org.springframework.mail.MailAuthenticationException: Authentication failed
```

**해결:**
- Gmail 앱 비밀번호 확인
- 2단계 인증 활성화 확인
- `application.yml`의 SMTP 설정 확인

### 2. Firebase 사용자 찾을 수 없음

**에러:**
```
FirebaseAuthException: USER_NOT_FOUND
```

**해결:**
- 이메일이 Firebase에 등록되어 있는지 확인
- 서버 로그인을 통해 사용자가 DB에 생성되었는지 확인

### 3. 이메일이 발송되지 않음

**해결:**
- SMTP 서버 연결 확인
- 방화벽/보안 그룹 설정 확인 (포트 587 허용)
- 로그에서 자세한 에러 메시지 확인

### 4. 이메일 링크 클릭 시 오류

**해결:**
- Firebase Console에서 승인된 도메인 확인
- `ActionCodeSettings`의 URL 설정 확인
- 프론트엔드에 이메일 인증 완료 페이지 구현

---

## 권장 사항

### 🎯 클라이언트 측 구현을 권장하는 이유

1. **간단함**: 코드 3줄로 구현 가능
2. **무료**: Firebase에서 이메일 발송 처리
3. **신뢰성**: Firebase 인프라 활용
4. **보안**: SMTP 자격 증명 노출 위험 없음

### 🔧 서버 측 구현이 필요한 경우

1. 커스텀 이메일 템플릿 필요
2. 이메일 발송 로직 완전 제어
3. 기존 SMTP 인프라 활용
4. 이메일 발송 이력 추적

---

## 참고 자료

- [Firebase Authentication - Email Verification](https://firebase.google.com/docs/auth/web/manage-users#send_a_user_a_verification_email)
- [Firebase Admin SDK - Email Action Links](https://firebase.google.com/docs/auth/admin/email-action-links)
- [React Native Firebase - Authentication](https://rnfirebase.io/auth/usage)
- [Spring Boot - Email](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)

---

## 요약

| 방법 | 장점 | 단점 | 권장 |
|------|------|------|------|
| **클라이언트** | 간단, 무료, 신뢰성 높음 | 커스텀 템플릿 제한적 | ⭐ 권장 |
| **서버** | 완전한 제어, 커스텀 템플릿 | SMTP 설정 필요, 복잡 | 특수한 경우만 |

**결론**: 대부분의 경우 **클라이언트 측 구현**을 사용하세요. 서버 측 구현은 커스텀 이메일 템플릿이 반드시 필요한 경우에만 사용하세요.
