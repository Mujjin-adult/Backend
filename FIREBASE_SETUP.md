# Firebase 설정 가이드

## 📋 개요

이 문서는 Incheon University Notice App에서 Firebase Authentication 및 FCM(Firebase Cloud Messaging)을 설정하는 방법을 안내합니다.

## 🔧 1. Firebase 프로젝트 설정

### 1.1 Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. **프로젝트 추가** 클릭
3. 프로젝트 이름 입력 (예: `incheon-notice-app`)
4. Google Analytics 설정 (선택사항)
5. 프로젝트 생성 완료

### 1.2 Firebase Authentication 활성화

1. Firebase Console → **Authentication** 메뉴
2. **시작하기** 클릭
3. **Sign-in method** 탭 선택
4. 활성화할 로그인 방법 설정:
   - ✅ **이메일/비밀번호** - 필수
   - ✅ **Google** - 선택 (소셜 로그인)
   - ✅ **기타 제공업체** - 선택

### 1.3 서비스 계정 키 다운로드

1. Firebase Console → **프로젝트 설정** (⚙️ 아이콘)
2. **서비스 계정** 탭 선택
3. **새 비공개 키 생성** 클릭
4. JSON 파일 다운로드 (이름: `firebase-credentials.json`)
5. **⚠️ 중요**: 이 파일을 프로젝트 루트 디렉토리에 배치

```bash
# 프로젝트 구조
incheon_univ_noti_app/
├── firebase-credentials.json  ← 여기에 배치
├── src/
├── build.gradle
└── ...
```

## 🔐 2. 환경변수 설정

### 2.1 로컬 개발 환경

프로젝트에 이미 `.env` 파일이 생성되어 있습니다:

```bash
# .env 파일
FCM_CREDENTIALS_PATH=./firebase-credentials.json
```

**현재 설정 상태:**
✅ `.env` 파일 생성됨
✅ `firebase-credentials.json` 파일 위치: 프로젝트 루트
✅ `.gitignore`에 등록됨 (Git 커밋 방지)

### 2.2 환경변수 확인

현재 설정된 환경변수:

```bash
# Firebase
FCM_CREDENTIALS_PATH=./firebase-credentials.json

# JWT (레거시)
JWT_SECRET=dev-secret-key-for-testing-only-change-in-production

# 데이터베이스
DATABASE_URL=jdbc:postgresql://localhost:5432/incheon_notice
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# 이메일 (Gmail SMTP)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# 기타
FRONTEND_URL=http://localhost:3000
CRAWLER_API_URL=http://localhost:8000
```

### 2.3 이메일 설정 (선택사항)

Gmail SMTP를 사용하는 경우:

1. Google 계정 → **보안** 설정
2. **2단계 인증** 활성화 필수
3. **앱 비밀번호** 생성:
   - [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
   - 앱 선택: **메일**
   - 기기 선택: **기타 (맞춤 이름)**
   - 이름 입력: `Incheon Notice App`
   - 생성된 16자리 비밀번호를 `.env`의 `MAIL_PASSWORD`에 입력

```bash
# .env
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop  # Gmail 앱 비밀번호 (공백 제거)
```

## 🚀 3. 애플리케이션 실행

### 3.1 개발 환경에서 실행

```bash
# Spring Boot 실행 (개발 프로필)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 IDE에서 실행 시 Active profiles에 'dev' 입력
```

### 3.2 Firebase 초기화 확인

애플리케이션 시작 시 다음 로그를 확인하세요:

```
✅ 성공:
INFO  c.i.n.c.FirebaseConfig : Firebase Admin SDK initialized successfully with config: ./firebase-credentials.json

❌ 실패:
ERROR c.i.n.c.FirebaseConfig : Failed to initialize Firebase Admin SDK: ...
```

## 🔍 4. 설정 확인 체크리스트

### 필수 설정

- [ ] Firebase 프로젝트 생성 완료
- [ ] Firebase Authentication 활성화 (이메일/비밀번호)
- [ ] `firebase-credentials.json` 파일 다운로드
- [ ] `firebase-credentials.json` 파일을 프로젝트 루트에 배치
- [ ] `.env` 파일 생성 및 `FCM_CREDENTIALS_PATH` 설정
- [ ] `.gitignore`에 `firebase-credentials.json` 포함 확인

### 선택 설정

- [ ] Google 소셜 로그인 활성화
- [ ] Gmail SMTP 설정 (이메일 발송 기능 사용 시)
- [ ] 프론트엔드 URL 설정 (`FRONTEND_URL`)

## 🏭 5. 프로덕션 환경 설정

### 5.1 환경변수 설정

프로덕션 환경에서는 `.env` 파일 대신 시스템 환경변수를 사용하세요:

```bash
# Linux/Mac
export FCM_CREDENTIALS_PATH=/secure/path/firebase-credentials.json
export JWT_SECRET=your-production-secret-key-256-bits-minimum
export DATABASE_URL=jdbc:postgresql://prod-db:5432/incheon_notice
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=secure_password

# Docker
docker run -e FCM_CREDENTIALS_PATH=/app/firebase-credentials.json \
           -e JWT_SECRET=your-production-secret \
           -v /secure/path/firebase-credentials.json:/app/firebase-credentials.json \
           your-image:latest
```

### 5.2 보안 권장사항

1. **서비스 계정 키 보안**
   ```bash
   # 프로덕션 서버에서 권한 설정
   chmod 600 /secure/path/firebase-credentials.json
   chown app-user:app-group /secure/path/firebase-credentials.json
   ```

2. **환경별 Firebase 프로젝트 분리**
   - 개발: `incheon-notice-dev`
   - 스테이징: `incheon-notice-staging`
   - 프로덕션: `incheon-notice-prod`

3. **Git 커밋 방지 확인**
   ```bash
   # 다음 파일들이 .gitignore에 포함되어 있는지 확인
   firebase-credentials.json
   .env
   *.json (서비스 계정 키)
   ```

## 🐛 6. 문제 해결

### 6.1 "Failed to initialize Firebase Admin SDK" 오류

**원인**: `firebase-credentials.json` 파일을 찾을 수 없음

**해결방법**:
```bash
# 1. 파일 존재 확인
ls -la firebase-credentials.json

# 2. 경로 확인
pwd
# 출력: /Users/your-name/project/incheon_univ_noti_app

# 3. .env 파일 확인
cat .env | grep FCM_CREDENTIALS_PATH
# 출력: FCM_CREDENTIALS_PATH=./firebase-credentials.json
```

### 6.2 "Invalid ID token" 오류

**원인**: Firebase ID Token이 만료되었거나 잘못됨

**해결방법**:
```javascript
// 클라이언트에서 토큰 강제 갱신
const idToken = await currentUser.getIdToken(true);  // true = 강제 갱신
```

### 6.3 이메일 발송 실패

**원인**: Gmail SMTP 설정 오류 또는 앱 비밀번호 미설정

**해결방법**:
1. Gmail 2단계 인증 활성화 확인
2. 앱 비밀번호 재생성
3. `.env` 파일의 `MAIL_PASSWORD` 업데이트
4. 공백 제거 확인: `abcdefghijklmnop` (올바름) vs `abcd efgh ijkl mnop` (잘못됨)

### 6.4 클라이언트 로그인 성공하지만 서버 API 401 에러

**원인**: Authorization 헤더 누락 또는 형식 오류

**해결방법**:
```javascript
// 올바른 헤더 형식
const response = await axios.get('/api/notices', {
  headers: {
    'Authorization': `Bearer ${idToken}`  // 'Bearer ' 접두사 필수
  }
});
```

## 📚 7. 참고 자료

- [Firebase Authentication 공식 문서](https://firebase.google.com/docs/auth)
- [Firebase Admin SDK 문서](https://firebase.google.com/docs/admin/setup)
- [Spring Boot + Firebase 가이드](https://firebase.google.com/docs/admin/setup#java)
- [FIREBASE_AUTHENTICATION_MIGRATION.md](./FIREBASE_AUTHENTICATION_MIGRATION.md) - 마이그레이션 가이드

## ✅ 설정 완료 확인

다음 명령어로 서버를 실행하고 Swagger UI에서 테스트하세요:

```bash
# 1. 서버 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 2. Swagger UI 접속
open http://localhost:8080/swagger-ui.html

# 3. Firebase Authentication으로 로그인 테스트
# - 클라이언트에서 Firebase SDK로 로그인
# - ID Token 발급받기
# - POST /api/auth/login 엔드포인트 호출
```

---

**설정 완료일**: 2025-11-19
**Firebase Admin SDK 버전**: 9.2.0
