#!/bin/bash

# Swagger API 테스트 데모 스크립트

BASE_URL="http://localhost:8080"

echo "╔════════════════════════════════════════════╗"
echo "║   Swagger API 테스트 데모                  ║"
echo "╚════════════════════════════════════════════╝"
echo ""

# 1. 회원가입
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1️⃣  회원가입 API 테스트"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📤 요청:"
cat << 'EOF'
POST /api/auth/signup
{
  "studentId": "202499999",
  "email": "swagger-demo@inu.ac.kr",
  "password": "password123",
  "name": "스웨거데모"
}
EOF
echo ""
echo "📥 응답:"

SIGNUP_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202499999",
    "email": "swagger-demo@inu.ac.kr",
    "password": "password123",
    "name": "스웨거데모"
  }')

echo "${SIGNUP_RESPONSE}" | jq '.'
echo ""

# 2. 로그인
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2️⃣  로그인 API 테스트"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📤 요청:"
cat << 'EOF'
POST /api/auth/login
{
  "email": "swagger-demo@inu.ac.kr",
  "password": "password123"
}
EOF
echo ""
echo "📥 응답:"

LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "swagger-demo@inu.ac.kr",
    "password": "password123"
  }')

echo "${LOGIN_RESPONSE}" | jq '.'

# 토큰 추출
ACCESS_TOKEN=$(echo "${LOGIN_RESPONSE}" | jq -r '.data.accessToken')

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3️⃣  JWT 토큰 정보"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔑 Access Token (처음 50자):"
echo "   ${ACCESS_TOKEN:0:50}..."
echo ""
echo "💡 Swagger에서 사용하는 방법:"
echo "   1. Swagger UI 상단의 🔓 Authorize 버튼 클릭"
echo "   2. Value 입력란에 다음 형식으로 입력:"
echo "      Bearer ${ACCESS_TOKEN:0:50}..."
echo "   3. Authorize 버튼 클릭"
echo "   4. Close 버튼으로 창 닫기"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 테스트 완료!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📖 Swagger UI에서 직접 테스트해보세요:"
echo "   👉 http://localhost:8080/swagger-ui.html"
echo ""
echo "📚 자세한 가이드:"
echo "   👉 SWAGGER_GUIDE.md 파일 참고"
echo ""
