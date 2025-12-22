# Firebase Authentication 활성화 단계별 가이드

## 📌 빠른 설정 (5분 소요)

### 1단계: Firebase Console 접속 및 로그인

1. 브라우저에서 다음 URL 접속:
   ```
   https://console.firebase.google.com/
   ```

2. Google 계정으로 로그인

3. 프로젝트 **`daon-47f9c`** 선택
   - 프로젝트가 보이지 않으면 프로젝트 생성 필요

### 2단계: Authentication 메뉴 찾기

1. 좌측 사이드바에서 **"빌드"** (Build) 섹션 찾기

2. **"Authentication"** 클릭

3. 처음 사용하는 경우:
   - **"시작하기"** 또는 **"Get Started"** 버튼 클릭

### 3단계: 이메일/비밀번호 인증 활성화 ⭐

1. **"Sign-in method"** 탭 클릭 (또는 "로그인 방법")

2. 로그인 제공업체 목록에서 **"이메일/비밀번호"** (Email/Password) 찾기

3. **"이메일/비밀번호"** 행 클릭

4. 설정 모달이 열림:
   - ✅ **"사용 설정"** 스위치를 켜기
   - ℹ️ "이메일 링크(비밀번호가 필요 없는 로그인)"는 선택사항
   - **"저장"** 버튼 클릭

5. 상태가 **"사용 설정됨"** (Enabled)으로 변경되었는지 확인

### 4단계: Web API Key 확인 ⭐

1. 좌측 상단의 **⚙️ (톱니바퀴)** 아이콘 클릭

2. **"프로젝트 설정"** (Project Settings) 선택

3. **"일반"** (General) 탭에 있는지 확인

4. 아래로 스크롤하여 **"웹 API 키"** (Web API Key) 찾기
   - 형식: `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` (39자)
   - 📋 이 키를 복사하여 메모장에 저장

### 5단계: 테스트 사용자 생성 (선택사항)

Firebase Console에서 직접 테스트 사용자를 만들 수 있습니다:

1. **Authentication** → **Users** 탭

2. **"사용자 추가"** (Add User) 버튼 클릭

3. 사용자 정보 입력:
   - 이메일: `test@inu.ac.kr`
   - 비밀번호: `testpassword123`

4. **"사용자 추가"** 클릭

5. 사용자 목록에 표시되는지 확인

## ✅ 설정 완료 확인

다음 항목이 모두 완료되었는지 확인:

- [ ] Firebase Console에 접속함
- [ ] 프로젝트 `daon-47f9c` 선택함
- [ ] Authentication 메뉴 접속함
- [ ] 이메일/비밀번호 인증 **사용 설정됨** 상태
- [ ] Web API Key 복사함 (AIza로 시작)
- [ ] (선택) 테스트 사용자 생성함

## 🧪 활성화 후 테스트

### 테스트 1: Python 스크립트로 확인

```bash
python3 test_firebase_direct.py
```

**기대 결과:**
```
✅ Firebase 사용자 생성: 성공 (firebasetest12345@inu.ac.kr)
```

이전에는 실패했지만, Authentication 활성화 후에는 성공해야 합니다.

### 테스트 2: Web API Key로 전체 플로우 테스트

```bash
./test_firebase_simple.sh
```

프롬프트에서 복사한 Web API Key를 붙여넣으세요.

**기대 결과:**
```
✅ Firebase 회원가입 성공
✅ Firebase Authentication 로그인 성공!
✅ 인증 API 호출 성공
```

### 테스트 3: 수동으로 확인 (Console에서 생성한 사용자)

```bash
# Web API Key를 변수에 설정
WEB_API_KEY="여기에_복사한_API_Key_붙여넣기"

# Firebase로 로그인하여 ID Token 발급
ID_TOKEN=$(curl -s -X POST \
  "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$WEB_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "testpassword123",
    "returnSecureToken": true
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['idToken'])")

# ID Token 확인
echo "ID Token: ${ID_TOKEN:0:50}..."

# 서버 로그인 API 테스트
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"idToken\": \"$ID_TOKEN\",
    \"fcmToken\": \"test-fcm-token\"
  }" | python3 -m json.tool
```

**기대 결과:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "idToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "email": "test@inu.ac.kr",
      "name": "...",
      "role": "USER"
    }
  }
}
```

## 🎯 활성화 완료 후 할 일

1. ✅ **서버 기능 테스트 완료**
2. 🔜 **클라이언트 앱에서 Firebase SDK 통합**
3. 🔜 **소셜 로그인 추가** (선택사항)

## 📚 추가 설정 (선택사항)

### 소셜 로그인 활성화

Google, Facebook 등 소셜 로그인도 활성화할 수 있습니다:

1. **Sign-in method** 탭
2. 원하는 제공업체 선택 (Google, Facebook, Apple 등)
3. 사용 설정 및 필요한 인증 정보 입력

### 이메일 템플릿 커스터마이징

1. **Authentication** → **Templates** 탭
2. 이메일 인증, 비밀번호 재설정 등의 템플릿 수정 가능

## ❓ 문제 해결

### "프로젝트가 보이지 않아요"

**해결방법:**
1. 올바른 Google 계정으로 로그인했는지 확인
2. Firebase 프로젝트를 먼저 생성해야 함
3. 프로젝트 생성: Firebase Console → "프로젝트 추가"

### "Web API Key를 찾을 수 없어요"

**해결방법:**
1. 프로젝트 설정 → 일반 탭
2. 아래로 스크롤
3. "내 앱" 섹션에서 웹 앱 추가 (처음 사용 시)
4. 웹 앱 추가 후 Web API Key 확인 가능

### "테스트가 여전히 실패해요"

**해결방법:**
1. 이메일/비밀번호 인증이 정말 "사용 설정됨" 상태인지 확인
2. Web API Key가 올바른지 확인 (AIza로 시작, 39자)
3. 서버 재시작: `docker-compose restart backend`
4. 캐시 문제: 몇 분 기다린 후 재시도

---

**작성일**: 2025-11-19
**프로젝트**: daon-47f9c
**다음 단계**: Web API Key 복사 → 테스트 실행
