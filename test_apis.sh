#!/bin/bash

# API 테스트 스크립트
BASE_URL="http://localhost:8080"
TOKEN=""

echo "================================"
echo "Incheon Notice API 테스트"
echo "================================"
echo ""

# Test 1: 회원가입
echo "Test 1: 회원가입 (POST /api/auth/signup)"
SIGNUP_RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId":"202112345",
    "email":"testuser999@inu.ac.kr",
    "password":"password123",
    "name":"김철수"
  }' -s)

echo "$SIGNUP_RESULT"
SUCCESS=$(echo "$SIGNUP_RESULT" | grep -o '"success":true')
if [ -n "$SUCCESS" ]; then
  echo "✅ PASS: 회원가입 성공"
else
  echo "❌ FAIL: 회원가입 실패"
fi
echo ""
sleep 1

# Test 2: 로그인
echo "Test 2: 로그인 (POST /api/auth/login)"
LOGIN_RESULT=$(curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"testuser999@inu.ac.kr",
    "password":"password123"
  }' -s)

echo "$LOGIN_RESULT"
SUCCESS=$(echo "$LOGIN_RESULT" | grep -o '"success":true')
if [ -n "$SUCCESS" ]; then
  TOKEN=$(echo "$LOGIN_RESULT" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
  echo "✅ PASS: 로그인 성공"
  echo "Token: $TOKEN"
else
  echo "❌ FAIL: 로그인 실패"
fi
echo ""
sleep 1

# Test 3: 내 정보 조회 (인증 필요)
echo "Test 3: 내 정보 조회 (GET /api/users/me)"
if [ -n "$TOKEN" ]; then
  ME_RESULT=$(curl -X GET "${BASE_URL}/api/users/me" \
    -H "Authorization: Bearer ${TOKEN}" -s)

  echo "$ME_RESULT"
  SUCCESS=$(echo "$ME_RESULT" | grep -o '"success":true')
  if [ -n "$SUCCESS" ]; then
    echo "✅ PASS: 내 정보 조회 성공"
  else
    echo "❌ FAIL: 내 정보 조회 실패"
  fi
else
  echo "❌ SKIP: 토큰이 없어 건너뜁니다"
fi
echo ""
sleep 1

# Test 4: 카테고리 목록 조회 (인증 불필요)
echo "Test 4: 카테고리 목록 조회 (GET /api/categories)"
CATEGORIES_RESULT=$(curl -X GET "${BASE_URL}/api/categories" -s)

echo "$CATEGORIES_RESULT"
SUCCESS=$(echo "$CATEGORIES_RESULT" | grep -o '"success":true')
if [ -n "$SUCCESS" ]; then
  echo "✅ PASS: 카테고리 목록 조회 성공"
else
  echo "❌ FAIL: 카테고리 목록 조회 실패"
fi
echo ""
sleep 1

# Test 5: 내 북마크 목록 조회 (인증 필요)
echo "Test 5: 북마크 목록 조회 (GET /api/bookmarks)"
if [ -n "$TOKEN" ]; then
  BOOKMARKS_RESULT=$(curl -X GET "${BASE_URL}/api/bookmarks?page=0&size=10" \
    -H "Authorization: Bearer ${TOKEN}" -s)

  echo "$BOOKMARKS_RESULT"
  SUCCESS=$(echo "$BOOKMARKS_RESULT" | grep -o '"success":true')
  if [ -n "$SUCCESS" ]; then
    echo "✅ PASS: 북마크 목록 조회 성공"
  else
    echo "❌ FAIL: 북마크 목록 조회 실패"
  fi
else
  echo "❌ SKIP: 토큰이 없어 건너뜁니다"
fi
echo ""
sleep 1

# Test 6: 내 구독 카테고리 조회 (인증 필요)
echo "Test 6: 구독 카테고리 조회 (GET /api/preferences/categories)"
if [ -n "$TOKEN" ]; then
  PREFS_RESULT=$(curl -X GET "${BASE_URL}/api/preferences/categories" \
    -H "Authorization: Bearer ${TOKEN}" -s)

  echo "$PREFS_RESULT"
  SUCCESS=$(echo "$PREFS_RESULT" | grep -o '"success":true')
  if [ -n "$SUCCESS" ]; then
    echo "✅ PASS: 구독 카테고리 조회 성공"
  else
    echo "❌ FAIL: 구독 카테고리 조회 실패"
  fi
else
  echo "❌ SKIP: 토큰이 없어 건너뜁니다"
fi
echo ""
sleep 1

# Test 7: 설정 변경 (인증 필요)
echo "Test 7: 사용자 설정 변경 (PUT /api/users/settings)"
if [ -n "$TOKEN" ]; then
  SETTINGS_RESULT=$(curl -X PUT "${BASE_URL}/api/users/settings" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "darkMode": true,
      "systemNotificationEnabled": false
    }' -s)

  echo "$SETTINGS_RESULT"
  SUCCESS=$(echo "$SETTINGS_RESULT" | grep -o '"success":true')
  if [ -n "$SUCCESS" ]; then
    echo "✅ PASS: 사용자 설정 변경 성공"
  else
    echo "❌ FAIL: 사용자 설정 변경 실패"
  fi
else
  echo "❌ SKIP: 토큰이 없어 건너뜁니다"
fi
echo ""

echo "================================"
echo "테스트 완료"
echo "================================"
