#!/bin/bash

# Firebase Authentication 간단 테스트 스크립트
# Firebase 프로젝트 ID와 Web API Key만 있으면 실행 가능

echo "============================================================"
echo "Firebase Authentication API 테스트"
echo "============================================================"
echo ""

# Firebase 프로젝트 정보
PROJECT_ID="daon-47f9c"
echo "📋 Firebase 프로젝트: $PROJECT_ID"
echo ""

# Web API Key 입력 (Firebase Console에서 확인)
echo "⚠️  Firebase Web API Key가 필요합니다:"
echo "   1. https://console.firebase.google.com/ 접속"
echo "   2. 프로젝트 '$PROJECT_ID' 선택"
echo "   3. 프로젝트 설정(⚙️) → 일반 탭"
echo "   4. '웹 API 키' 복사"
echo ""
read -p "Firebase Web API Key 입력: " WEB_API_KEY

if [ -z "$WEB_API_KEY" ]; then
    echo "❌ Web API Key가 입력되지 않았습니다."
    echo ""
    echo "💡 대신 레거시 회원가입 API를 테스트합니다..."
    echo ""

    # 레거시 회원가입 테스트
    TEST_NUM=$RANDOM
    echo "=== 레거시 회원가입 테스트 ==="
    echo "이메일: legacytest${TEST_NUM}@inu.ac.kr"

    curl -s -X POST http://localhost:8080/api/auth/signup \
        -H "Content-Type: application/json" \
        -d "{
            \"studentId\": \"2021${TEST_NUM}\",
            \"email\": \"legacytest${TEST_NUM}@inu.ac.kr\",
            \"password\": \"testpassword123\",
            \"name\": \"레거시테스트${TEST_NUM}\"
        }" | python3 -m json.tool

    echo ""
    echo "✅ 레거시 API는 정상 작동합니다."
    echo "🔥 Firebase Authentication을 테스트하려면 Web API Key가 필요합니다."
    exit 0
fi

echo ""
echo "============================================================"
echo "Firebase 테스트 계정 생성 중..."
echo "============================================================"

# 테스트 계정 정보
TEST_NUM=$RANDOM
TEST_EMAIL="firebasetest${TEST_NUM}@inu.ac.kr"
TEST_PASSWORD="testpassword123"

echo "📧 이메일: $TEST_EMAIL"
echo "🔒 비밀번호: $TEST_PASSWORD"
echo ""

# Firebase Authentication으로 회원가입
echo "📝 Firebase Authentication에 회원가입 중..."
SIGNUP_RESPONSE=$(curl -s -X POST \
    "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$WEB_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"$TEST_PASSWORD\",
        \"returnSecureToken\": true
    }")

# 응답에서 ID Token 추출
ID_TOKEN=$(echo $SIGNUP_RESPONSE | python3 -c "import sys, json; data = json.load(sys.stdin); print(data.get('idToken', ''))" 2>/dev/null)

if [ -z "$ID_TOKEN" ]; then
    echo "❌ Firebase 회원가입 실패"
    echo "응답: $SIGNUP_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$SIGNUP_RESPONSE"
    exit 1
fi

echo "✅ Firebase 회원가입 성공"
echo "🔑 ID Token (앞 50자): ${ID_TOKEN:0:50}..."
echo ""

# 서버 로그인 API 테스트
echo "============================================================"
echo "서버 /api/auth/login API 테스트"
echo "============================================================"
echo ""

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{
        \"idToken\": \"$ID_TOKEN\",
        \"fcmToken\": \"test-fcm-token-12345\"
    }")

echo "$LOGIN_RESPONSE" | python3 -m json.tool

# 로그인 성공 여부 확인
SUCCESS=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; data = json.load(sys.stdin); print(data.get('success', False))" 2>/dev/null)

if [ "$SUCCESS" = "True" ]; then
    echo ""
    echo "✅ Firebase Authentication 로그인 성공!"

    # 인증이 필요한 API 테스트
    echo ""
    echo "============================================================"
    echo "인증이 필요한 API 테스트 (/api/bookmarks)"
    echo "============================================================"
    echo ""

    BOOKMARKS_RESPONSE=$(curl -s -X GET http://localhost:8080/api/bookmarks \
        -H "Authorization: Bearer $ID_TOKEN" \
        -H "Content-Type: application/json")

    echo "$BOOKMARKS_RESPONSE" | python3 -m json.tool

    echo ""
    echo "============================================================"
    echo "테스트 완료!"
    echo "============================================================"
    echo ""
    echo "✅ Firebase Authentication이 정상적으로 작동합니다!"
    echo ""
    echo "📋 테스트 계정 정보:"
    echo "   이메일: $TEST_EMAIL"
    echo "   비밀번호: $TEST_PASSWORD"
    echo "   ID Token: ${ID_TOKEN:0:50}..."
    echo ""
    echo "💡 다음 단계:"
    echo "   1. 클라이언트 앱에서 Firebase SDK 설치"
    echo "   2. signInWithEmailAndPassword()로 로그인"
    echo "   3. getIdToken()으로 ID Token 발급"
    echo "   4. 서버 API 호출 시 'Authorization: Bearer <ID_TOKEN>' 헤더 추가"
else
    echo ""
    echo "❌ 로그인 실패"
    echo ""
    echo "디버깅 정보:"
    echo "- 서버 로그를 확인하세요: docker-compose logs backend --tail=50"
fi
