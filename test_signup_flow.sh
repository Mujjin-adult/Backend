#!/bin/bash

# 띠링인캠퍼스 회원가입 플로우 테스트 스크립트
# 이 스크립트는 회원가입이 Firebase에 제대로 저장되는지 테스트합니다

echo "=========================================="
echo "🧪 회원가입 플로우 테스트"
echo "=========================================="
echo ""

# 랜덤 테스트 번호 생성
TEST_NUM=$(date +%s)
EMAIL="test${TEST_NUM}@inu.ac.kr"
PASSWORD="testpass123"
STUDENT_ID="2021${TEST_NUM: -5}"
NAME="테스트사용자${TEST_NUM: -3}"

echo "📝 테스트 계정 정보:"
echo "  - 이메일: $EMAIL"
echo "  - 비밀번호: $PASSWORD"
echo "  - 학번: $STUDENT_ID"
echo "  - 이름: $NAME"
echo ""

# 1. 회원가입 API 호출
echo "=========================================="
echo "STEP 1: 서버 회원가입 API 호출"
echo "=========================================="
SIGNUP_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"studentId\": \"$STUDENT_ID\",
    \"name\": \"$NAME\"
  }")

echo "응답:"
echo "$SIGNUP_RESPONSE" | jq .

# 성공 여부 확인
SUCCESS=$(echo "$SIGNUP_RESPONSE" | jq -r '.success')

if [ "$SUCCESS" = "true" ]; then
    echo ""
    echo "✅ STEP 1 성공: 회원가입 완료"
    echo ""

    USER_ID=$(echo "$SIGNUP_RESPONSE" | jq -r '.data.id')
    echo "생성된 사용자 ID: $USER_ID"
    echo ""

    echo "=========================================="
    echo "다음 단계 안내"
    echo "=========================================="
    echo ""
    echo "⚠️  이제 클라이언트에서 다음 작업을 수행해야 합니다:"
    echo ""
    echo "1️⃣  Firebase 로그인:"
    echo "   import auth from '@react-native-firebase/auth';"
    echo "   const userCredential = await auth().signInWithEmailAndPassword('$EMAIL', '$PASSWORD');"
    echo ""
    echo "2️⃣  ID Token 발급:"
    echo "   const idToken = await userCredential.user.getIdToken();"
    echo ""
    echo "3️⃣  FCM Token 발급:"
    echo "   import messaging from '@react-native-firebase/messaging';"
    echo "   const fcmToken = await messaging().getToken();"
    echo ""
    echo "4️⃣  서버 로그인 API 호출:"
    echo "   curl -X POST 'http://localhost:8080/api/auth/login' \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"idToken\": \"YOUR_ID_TOKEN\", \"fcmToken\": \"YOUR_FCM_TOKEN\"}'"
    echo ""
    echo "=========================================="
    echo "Firebase Console 확인"
    echo "=========================================="
    echo ""
    echo "🔍 Firebase Console에서 확인:"
    echo "   https://console.firebase.google.com/"
    echo ""
    echo "✅ Authentication > Users 탭에서 다음 이메일이 등록되었는지 확인:"
    echo "   📧 $EMAIL"
    echo ""

else
    echo ""
    echo "❌ STEP 1 실패: 회원가입 실패"
    echo ""
    ERROR_MESSAGE=$(echo "$SIGNUP_RESPONSE" | jq -r '.message')
    echo "에러 메시지: $ERROR_MESSAGE"
    echo ""
fi

echo "=========================================="
echo "테스트 완료"
echo "=========================================="
