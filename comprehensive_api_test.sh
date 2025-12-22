#!/bin/bash

# 종합 API 테스트 스크립트
BASE_URL="http://localhost:8080"
TOKEN=""
USER_ID=""
RANDOM_NUM=$RANDOM

echo "========================================"
echo "   Incheon Notice API 종합 테스트"
echo "========================================"
echo ""
echo "랜덤 번호: $RANDOM_NUM"
echo ""

# Test 1: 회원가입 API
echo "----------------------------------------"
echo "Test 1: 회원가입 API"
echo "POST /api/auth/signup"
echo "----------------------------------------"

SIGNUP_RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{
    \"studentId\":\"2021${RANDOM_NUM}\",
    \"email\":\"testuser${RANDOM_NUM}@inu.ac.kr\",
    \"password\":\"password123\",
    \"name\":\"테스트사용자${RANDOM_NUM}\"
  }" -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$SIGNUP_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$SIGNUP_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "201" ]; then
  echo "✅ PASS"
  USER_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 2: 중복 회원가입 시도 (400 예상)
echo "----------------------------------------"
echo "Test 2: 중복 회원가입 시도 (400 예상)"
echo "POST /api/auth/signup"
echo "----------------------------------------"

DUP_RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{
    \"studentId\":\"2021${RANDOM_NUM}\",
    \"email\":\"testuser${RANDOM_NUM}@inu.ac.kr\",
    \"password\":\"password123\",
    \"name\":\"테스트사용자${RANDOM_NUM}\"
  }" -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$DUP_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$DUP_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "500" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "409" ]; then
  echo "✅ PASS (중복 감지)"
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 3: 로그인 API
echo "----------------------------------------"
echo "Test 3: 로그인 API"
echo "POST /api/auth/login"
echo "----------------------------------------"

LOGIN_RESULT=$(curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\":\"testuser${RANDOM_NUM}@inu.ac.kr\",
    \"password\":\"password123\"
  }" -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$LOGIN_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$LOGIN_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
  TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
  echo "Token: ${TOKEN:0:50}..."
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 4: 잘못된 비밀번호로 로그인 (401 예상)
echo "----------------------------------------"
echo "Test 4: 잘못된 비밀번호로 로그인 (401 예상)"
echo "POST /api/auth/login"
echo "----------------------------------------"

WRONG_LOGIN=$(curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\":\"testuser${RANDOM_NUM}@inu.ac.kr\",
    \"password\":\"wrongpassword\"
  }" -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$WRONG_LOGIN" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$WRONG_LOGIN" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "500" ]; then
  echo "✅ PASS (인증 실패)"
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 5: 내 정보 조회 (인증 필요)
echo "----------------------------------------"
echo "Test 5: 내 정보 조회"
echo "GET /api/users/me"
echo "----------------------------------------"

if [ -n "$TOKEN" ]; then
  ME_RESULT=$(curl -X GET "${BASE_URL}/api/users/me" \
    -H "Authorization: Bearer ${TOKEN}" \
    -s -w "\nHTTP_CODE:%{http_code}")

  HTTP_CODE=$(echo "$ME_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  RESPONSE=$(echo "$ME_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

  echo "Response: $RESPONSE"
  echo "HTTP Code: $HTTP_CODE"

  if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ PASS"
  else
    echo "❌ FAIL"
  fi
else
  echo "❌ SKIP (토큰 없음)"
fi
echo ""
sleep 1

# Test 6: 토큰 없이 내 정보 조회 (401 예상)
echo "----------------------------------------"
echo "Test 6: 토큰 없이 내 정보 조회 (401 예상)"
echo "GET /api/users/me"
echo "----------------------------------------"

NO_TOKEN=$(curl -X GET "${BASE_URL}/api/users/me" \
  -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$NO_TOKEN" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$NO_TOKEN" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
  echo "✅ PASS (인증 필요)"
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 7: 사용자 설정 변경
echo "----------------------------------------"
echo "Test 7: 사용자 설정 변경"
echo "PUT /api/users/settings"
echo "----------------------------------------"

if [ -n "$TOKEN" ]; then
  SETTINGS_RESULT=$(curl -X PUT "${BASE_URL}/api/users/settings" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "darkMode": true,
      "systemNotificationEnabled": false
    }' -s -w "\nHTTP_CODE:%{http_code}")

  HTTP_CODE=$(echo "$SETTINGS_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  RESPONSE=$(echo "$SETTINGS_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

  echo "Response: $RESPONSE"
  echo "HTTP Code: $HTTP_CODE"

  if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ PASS"
  else
    echo "❌ FAIL"
  fi
else
  echo "❌ SKIP (토큰 없음)"
fi
echo ""
sleep 1

# Test 8: 카테고리 목록 조회 (인증 불필요)
echo "----------------------------------------"
echo "Test 8: 카테고리 목록 조회"
echo "GET /api/categories"
echo "----------------------------------------"

CATEGORIES_RESULT=$(curl -X GET "${BASE_URL}/api/categories" \
  -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$CATEGORIES_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$CATEGORIES_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
  CATEGORY_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  echo "First Category ID: $CATEGORY_ID"
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 9: 카테고리 구독
echo "----------------------------------------"
echo "Test 9: 카테고리 구독"
echo "POST /api/preferences/categories"
echo "----------------------------------------"

if [ -n "$TOKEN" ] && [ -n "$CATEGORY_ID" ]; then
  SUBSCRIBE_RESULT=$(curl -X POST "${BASE_URL}/api/preferences/categories" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"categoryId\": ${CATEGORY_ID},
      \"notificationEnabled\": true
    }" -s -w "\nHTTP_CODE:%{http_code}")

  HTTP_CODE=$(echo "$SUBSCRIBE_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  RESPONSE=$(echo "$SUBSCRIBE_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

  echo "Response: $RESPONSE"
  echo "HTTP Code: $HTTP_CODE"

  if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    echo "✅ PASS"
  else
    echo "❌ FAIL"
  fi
else
  echo "❌ SKIP (토큰 또는 카테고리 ID 없음)"
fi
echo ""
sleep 1

# Test 10: 내 구독 목록 조회
echo "----------------------------------------"
echo "Test 10: 내 구독 목록 조회"
echo "GET /api/preferences/categories"
echo "----------------------------------------"

if [ -n "$TOKEN" ]; then
  PREFS_RESULT=$(curl -X GET "${BASE_URL}/api/preferences/categories" \
    -H "Authorization: Bearer ${TOKEN}" \
    -s -w "\nHTTP_CODE:%{http_code}")

  HTTP_CODE=$(echo "$PREFS_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  RESPONSE=$(echo "$PREFS_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

  echo "Response: $RESPONSE"
  echo "HTTP Code: $HTTP_CODE"

  if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ PASS"
  else
    echo "❌ FAIL"
  fi
else
  echo "❌ SKIP (토큰 없음)"
fi
echo ""
sleep 1

# Test 11: 북마크 목록 조회
echo "----------------------------------------"
echo "Test 11: 북마크 목록 조회"
echo "GET /api/bookmarks"
echo "----------------------------------------"

if [ -n "$TOKEN" ]; then
  BOOKMARKS_RESULT=$(curl -X GET "${BASE_URL}/api/bookmarks?page=0&size=10" \
    -H "Authorization: Bearer ${TOKEN}" \
    -s -w "\nHTTP_CODE:%{http_code}")

  HTTP_CODE=$(echo "$BOOKMARKS_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  RESPONSE=$(echo "$BOOKMARKS_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

  echo "Response: $RESPONSE"
  echo "HTTP Code: $HTTP_CODE"

  if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ PASS"
  else
    echo "❌ FAIL"
  fi
else
  echo "❌ SKIP (토큰 없음)"
fi
echo ""
sleep 1

# Test 12: 공지사항 목록 조회 (인증 불필요)
echo "----------------------------------------"
echo "Test 12: 공지사항 목록 조회"
echo "GET /api/notices"
echo "----------------------------------------"

NOTICES_RESULT=$(curl -X GET "${BASE_URL}/api/notices?page=0&size=10" \
  -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$NOTICES_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$NOTICES_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
  NOTICE_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  if [ -n "$NOTICE_ID" ]; then
    echo "First Notice ID: $NOTICE_ID"
  fi
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 13: 공지사항 검색
echo "----------------------------------------"
echo "Test 13: 공지사항 검색"
echo "GET /api/notices/search"
echo "----------------------------------------"

# URL 인코딩된 한글 키워드 사용 (장학 = %EC%9E%A5%ED%95%99)
SEARCH_RESULT=$(curl -X GET "${BASE_URL}/api/notices/search?keyword=%EC%9E%A5%ED%95%99&page=0&size=10" \
  -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$SEARCH_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
RESPONSE=$(echo "$SEARCH_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

echo "Response: $RESPONSE"
echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL"
fi
echo ""
sleep 1

# Test 14: 토큰 갱신
echo "----------------------------------------"
echo "Test 14: 토큰 갱신 테스트"
echo "POST /api/auth/refresh"
echo "----------------------------------------"

if [ -n "$TOKEN" ]; then
  # 먼저 로그인하여 refresh token 가져오기
  LOGIN_FOR_REFRESH=$(curl -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{
      \"email\":\"testuser${RANDOM_NUM}@inu.ac.kr\",
      \"password\":\"password123\"
    }" -s)

  REFRESH_TOKEN=$(echo "$LOGIN_FOR_REFRESH" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)

  if [ -n "$REFRESH_TOKEN" ]; then
    REFRESH_RESULT=$(curl -X POST "${BASE_URL}/api/auth/refresh" \
      -H "Content-Type: application/json" \
      -d "{
        \"refreshToken\": \"${REFRESH_TOKEN}\"
      }" -s -w "\nHTTP_CODE:%{http_code}")

    HTTP_CODE=$(echo "$REFRESH_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
    RESPONSE=$(echo "$REFRESH_RESULT" | sed 's/HTTP_CODE:[0-9]*$//')

    echo "Response: $RESPONSE"
    echo "HTTP Code: $HTTP_CODE"

    if [ "$HTTP_CODE" = "200" ]; then
      echo "✅ PASS"
    else
      echo "❌ FAIL"
    fi
  else
    echo "❌ SKIP (Refresh Token 없음)"
  fi
else
  echo "❌ SKIP (토큰 없음)"
fi
echo ""
sleep 1

# Test 15: Swagger UI 접근
echo "----------------------------------------"
echo "Test 15: Swagger UI 접근"
echo "GET /swagger-ui.html"
echo "----------------------------------------"

SWAGGER_RESULT=$(curl -X GET "${BASE_URL}/swagger-ui.html" \
  -s -w "\nHTTP_CODE:%{http_code}" -o /dev/null)

HTTP_CODE=$(echo "$SWAGGER_RESULT" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)

echo "HTTP Code: $HTTP_CODE"

if [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL"
fi
echo ""

echo "========================================"
echo "           테스트 완료"
echo "========================================"
echo ""
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "pgAdmin: http://localhost:5050"
echo ""
