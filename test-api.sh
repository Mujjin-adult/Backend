#!/bin/bash

# 인천대학교 공지사항 백엔드 API 테스트 스크립트

BASE_URL="http://localhost:8080"

echo "=============================================="
echo "인천대학교 공지사항 백엔드 API 테스트"
echo "=============================================="
echo ""

# 1. 회원가입 테스트
echo "1️⃣  회원가입 테스트..."
SIGNUP_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202312345",
    "email": "test@inu.ac.kr",
    "password": "password123",
    "name": "홍길동"
  }')

echo "응답: ${SIGNUP_RESPONSE}"
echo ""

# 2. 로그인 테스트
echo "2️⃣  로그인 테스트..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@inu.ac.kr",
    "password": "password123"
  }')

echo "응답: ${LOGIN_RESPONSE}"
echo ""

# 토큰 추출
ACCESS_TOKEN=$(echo "${LOGIN_RESPONSE}" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "❌ 로그인 실패 또는 토큰을 찾을 수 없습니다."
else
  echo "✅ 로그인 성공! 토큰: ${ACCESS_TOKEN:0:50}..."
fi

echo ""
echo "=============================================="
echo "테스트 완료!"
echo "=============================================="
echo ""
echo "✨ Swagger UI에서 더 많은 API를 테스트해보세요:"
echo "   👉 http://localhost:8080/swagger-ui.html"
