# FCM (Firebase Cloud Messaging) 토큰 가이드

## 🤔 FCM 토큰이 뭔가요?

**FCM 토큰**은 푸시 알림을 받을 특정 스마트폰을 식별하는 고유 문자열입니다.

```
예시 FCM 토큰:
eXAMPLE123:APA91bHun4MxP5egoKMwt4wrj9PFNGzm8xLKF...
```

---

## ✅ 핵심 정리

### 지금 Swagger에서 테스트하는 경우

**→ FCM 토큰을 입력하지 않아도 됩니다!**

```json
{
  "email": "test@inu.ac.kr",
  "password": "password123"
}
```

✅ 이렇게만 입력하면 정상적으로 로그인됩니다.

---

## 📱 시나리오별 가이드

### 1. Swagger UI 테스트

**목적**: API가 잘 작동하는지 확인

**방법**: FCM 토큰 필드를 **비워두거나 아예 제거**

```json
{
  "email": "myemail@inu.ac.kr",
  "password": "password123"
}
```

**결과**: ✅ 정상 로그인 (푸시 알림만 안 받음)

---

### 2. Flutter 앱 개발 시

**목적**: 실제 푸시 알림 받기

**방법**: Firebase SDK가 자동으로 토큰 발급

```dart
// 1. Firebase 패키지 추가 (pubspec.yaml)
dependencies:
  firebase_core: ^2.24.2
  firebase_messaging: ^14.7.9

// 2. FCM 토큰 받기
import 'package:firebase_messaging/firebase_messaging.dart';

Future<String?> getFCMToken() async {
  // Firebase 초기화
  await Firebase.initializeApp();

  // 권한 요청 (iOS)
  NotificationSettings settings =
    await FirebaseMessaging.instance.requestPermission();

  if (settings.authorizationStatus == AuthorizationStatus.authorized) {
    // FCM 토큰 받기
    String? token = await FirebaseMessaging.instance.getToken();
    print('FCM Token: $token');
    return token;
  }

  return null;
}

// 3. 로그인 API 호출 시 토큰 포함
Future<void> login(String email, String password) async {
  // FCM 토큰 받기
  String? fcmToken = await getFCMToken();

  // 로그인 API 호출
  final response = await http.post(
    Uri.parse('http://your-server.com/api/auth/login'),
    headers: {'Content-Type': 'application/json'},
    body: jsonEncode({
      'email': email,
      'password': password,
      'fcmToken': fcmToken,  // 자동 발급받은 토큰
    }),
  );

  // 응답 처리...
}
```

**결과**: ✅ 로그인 + 푸시 알림 수신 가능

---

### 3. 기능 테스트 (푸시 없이)

**목적**: FCM 토큰 저장 기능만 테스트

**방법**: 임의의 더미 문자열 사용

```json
{
  "email": "test@inu.ac.kr",
  "password": "password123",
  "fcmToken": "dummy-test-token-12345-for-testing"
}
```

**결과**: ✅ 토큰이 DB에 저장됨 (실제 푸시는 안 감)

---

## 🔥 Firebase 프로젝트 설정 (실제 푸시 알림 사용 시)

### Step 1: Firebase 프로젝트 생성

1. https://console.firebase.google.com/ 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름 입력: `인천대학교 공지사항`
4. Google Analytics 비활성화 (선택사항)
5. "프로젝트 만들기" 클릭

### Step 2: Android 앱 추가 (Flutter)

1. Firebase Console에서 ⚙️ → 프로젝트 설정
2. "Android 앱에 Firebase 추가" 클릭
3. Android 패키지 이름 입력: `com.inu.notice` (예시)
4. `google-services.json` 다운로드
5. Flutter 프로젝트의 `android/app/` 폴더에 저장

### Step 3: iOS 앱 추가 (Flutter)

1. "iOS 앱에 Firebase 추가" 클릭
2. iOS 번들 ID 입력: `com.inu.notice` (예시)
3. `GoogleService-Info.plist` 다운로드
4. Flutter 프로젝트의 `ios/Runner/` 폴더에 저장

### Step 4: 서비스 계정 키 생성 (백엔드용)

1. Firebase Console → 프로젝트 설정 → 서비스 계정
2. "새 비공개 키 생성" 클릭
3. JSON 파일 다운로드
4. 파일명을 `firebase-credentials.json`으로 변경
5. 백엔드 프로젝트 루트에 저장

```bash
# 백엔드 프로젝트 루트에 저장
mv ~/Downloads/incheon-notice-xxxxx.json /path/to/backend/firebase-credentials.json
```

6. `.gitignore`에 추가 (보안!)

```bash
echo "firebase-credentials.json" >> .gitignore
```

---

## 🧪 테스트 시나리오

### 테스트 1: FCM 토큰 없이 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123"
  }'
```

**예상 결과**: ✅ 로그인 성공

### 테스트 2: FCM 토큰과 함께 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123",
    "fcmToken": "test-dummy-token-12345"
  }'
```

**예상 결과**: ✅ 로그인 성공 + FCM 토큰 저장

### 테스트 3: 데이터베이스에서 FCM 토큰 확인

```bash
# pgAdmin에서 확인
http://localhost:5050

# 또는 터미널에서
docker exec incheon-notice-db psql -U postgres -d incheon_notice \
  -c "SELECT email, fcm_token FROM users WHERE email = 'test@inu.ac.kr';"
```

**예상 결과**:
```
        email        |       fcm_token
---------------------+------------------------
 test@inu.ac.kr      | test-dummy-token-12345
```

---

## 💡 자주 묻는 질문 (FAQ)

### Q1: FCM 토큰을 입력하지 않으면 어떻게 되나요?

**A**: 아무 문제 없습니다! 정상적으로 로그인되고, 푸시 알림만 받지 못합니다.

### Q2: FCM 토큰은 언제 필요한가요?

**A**: 푸시 알림을 받고 싶을 때만 필요합니다.

- ❌ 일반 로그인 → 불필요
- ✅ 새 공지사항 푸시 알림 → 필요

### Q3: FCM 토큰을 수동으로 입력할 수 있나요?

**A**: 테스트 목적이라면 가능합니다. 하지만 실제 푸시는 안 갑니다.

```json
{
  "fcmToken": "any-string-you-want-for-testing"
}
```

### Q4: Swagger에서 FCM 토큰 필드가 보여요

**A**: 선택적 필드이므로 비워두면 됩니다.

**Swagger UI에서 보이는 예시:**
```json
{
  "email": "string",
  "password": "string",
  "fcmToken": "string"  // ← 이 줄을 삭제하거나 비워두세요
}
```

**실제 입력:**
```json
{
  "email": "test@inu.ac.kr",
  "password": "password123"
}
```

### Q5: FCM 토큰은 어떻게 생겼나요?

**A**: 긴 영숫자 문자열입니다 (약 150자 이상).

```
예시:
eXAMPLE:APA91bHun4MxP5egoKMwt4wrj9PFNGzm8xLKFcNzTfR...
```

### Q6: FCM 토큰이 변경될 수 있나요?

**A**: 네! 앱 재설치, 데이터 삭제 등의 경우 변경됩니다.

→ Flutter 앱에서 토큰이 변경되면 다시 로그인하거나 별도 API로 업데이트해야 합니다.

---

## 🎯 요약

| 상황 | FCM 토큰 | 결과 |
|------|----------|------|
| Swagger 테스트 | ❌ 입력 안 함 | ✅ 로그인 성공 |
| Flutter 앱 개발 | ✅ 자동 발급 | ✅ 로그인 + 푸시 알림 |
| 기능 테스트 | ⚠️ 더미 값 | ✅ 로그인 + DB 저장만 |

---

## 📚 추가 자료

- **Firebase 공식 문서**: https://firebase.google.com/docs/cloud-messaging
- **FlutterFire**: https://firebase.flutter.dev/
- **Swagger 가이드**: SWAGGER_GUIDE.md
- **pgAdmin 가이드**: PGADMIN_GUIDE.md

---

**핵심: FCM 토큰은 선택사항입니다. 지금은 입력하지 않아도 됩니다!** ✨
