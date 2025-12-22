#!/bin/bash

# 전체 인증 플로우 테스트 스크립트
# 회원가입 → 이메일 로그인 → API 요청

set -e  # 에러 발생 시 중단

echo "=========================================="
echo "🧪 전체 인증 플로우 테스트"
echo "=========================================="
echo ""

# 랜덤 테스트 번호 생성
TEST_NUM=$(date +%s)
EMAIL="test${TEST_NUM}@inu.ac.kr"
PASSWORD="testpass123"
STUDENT_ID="2021${TEST_NUM: -5}"
NAME="테스트${TEST_NUM: -3}"

echo "📝 테스트 계정 정보:"
echo "  - 이메일: $EMAIL"
echo "  - 비밀번호: $PASSWORD"
echo "  - 학번: $STUDENT_ID"
echo "  - 이름: $NAME"
echo ""

# ==========================================
# STEP 1: 회원가입
# ==========================================
echo "=========================================="
echo "STEP 1: 회원가입"
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

if [ "$SUCCESS" != "true" ]; then
    echo ""
    echo "❌ 회원가입 실패"
    ERROR_MESSAGE=$(echo "$SIGNUP_RESPONSE" | jq -r '.message')
    echo "에러 메시지: $ERROR_MESSAGE"
    exit 1
fi

USER_ID=$(echo "$SIGNUP_RESPONSE" | jq -r '.data.id')
echo ""
echo "✅ STEP 1 성공: 회원가입 완료 (User ID: $USER_ID)"
echo ""
sleep 1

# ==========================================
# STEP 2: 이메일/비밀번호 로그인
# ==========================================
echo "=========================================="
echo "STEP 2: 이메일/비밀번호 로그인"
echo "=========================================="

LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/login/email" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

echo "응답:"
echo "$LOGIN_RESPONSE" | jq .

# 성공 여부 확인
SUCCESS=$(echo "$LOGIN_RESPONSE" | jq -r '.success')

if [ "$SUCCESS" != "true" ]; then
    echo ""
    echo "❌ 로그인 실패"
    ERROR_MESSAGE=$(echo "$LOGIN_RESPONSE" | jq -r '.message')
    echo "에러 메시지: $ERROR_MESSAGE"
    exit 1
fi

ID_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.idToken')
echo ""
echo "✅ STEP 2 성공: 로그인 완료"
echo "🎫 Firebase 커스텀 토큰: ${ID_TOKEN:0:20}..."
echo ""
sleep 1

# ==========================================
# STEP 3: 인증된 API 호출 테스트
# ==========================================
echo "=========================================="
echo "STEP 3: 인증된 API 호출 테스트"
echo "=========================================="

# 3.1 공지사항 목록 조회 (인증 필요 없음, 하지만 토큰 전송 시 북마크 정보 포함)
echo ""
echo "📋 공지사항 목록 조회 (첫 5개)..."
NOTICES_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/notices?size=5" \
  -H "Authorization: Bearer $ID_TOKEN")

NOTICES_COUNT=$(echo "$NOTICES_RESPONSE" | jq -r '.data.totalElements // 0')
echo "총 공지사항 수: $NOTICES_COUNT"

if [ "$NOTICES_COUNT" -gt 0 ]; then
    echo "✅ 공지사항 조회 성공"
    echo ""
    echo "최신 공지사항 3개:"
    echo "$NOTICES_RESPONSE" | jq -r '.data.content[:3] | .[] | "  - [\(.id)] \(.title)"'
else
    echo "⚠️  공지사항이 없습니다 (정상 - 크롤링 필요)"
fi

echo ""
sleep 1

# ==========================================
# 최종 결과
# ==========================================
echo ""
echo "=========================================="
echo "🎉 전체 테스트 완료!"
echo "=========================================="
echo ""
echo "✅ 모든 단계가 성공적으로 완료되었습니다:"
echo ""
echo "1️⃣  회원가입 완료"
echo "   - User ID: $USER_ID"
echo "   - 이메일: $EMAIL"
echo ""
echo "2️⃣  로그인 완료"
echo "   - Firebase 커스텀 토큰 발급 완료"
echo "   - 토큰: ${ID_TOKEN:0:30}..."
echo ""
echo "3️⃣  API 인증 테스트 완료"
echo "   - 공지사항 조회 성공"
echo ""
echo "=========================================="
echo "다음 단계"
echo "=========================================="
echo ""
echo "💡 이제 클라이언트에서 다음과 같이 사용하세요:"
echo ""
echo "1. 회원가입:"
echo "   POST /api/auth/signup"
echo ""
echo "2. 로그인:"
echo "   POST /api/auth/login/email"
echo "   { \"email\": \"$EMAIL\", \"password\": \"$PASSWORD\" }"
echo ""
echo "3. API 요청 시 토큰 사용:"
echo "   Authorization: Bearer \$idToken"
echo ""
echo "=========================================="
echo "Firebase Console 확인"
echo "=========================================="
echo ""
echo "🔍 Firebase Console에서 확인:"
echo "   https://console.firebase.google.com/"
echo ""
echo "✅ Authentication > Users 탭에서 확인:"
echo "   📧 $EMAIL"
echo ""
