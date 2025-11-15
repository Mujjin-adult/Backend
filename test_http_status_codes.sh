#!/bin/bash

# HTTP 상태 코드 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "========================================"
echo "   HTTP 상태 코드 테스트"
echo "========================================"
echo ""

# Test 1: 중복 이메일 (409 Conflict 예상)
echo "Test 1: 중복 이메일"
echo "Expected: 409 Conflict"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"999999999","email":"test@inu.ac.kr","password":"password123","name":"테스트"}' \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "409" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 409, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 2: 잘못된 이메일 도메인 (400 Bad Request 예상)
echo "Test 2: 잘못된 이메일 도메인 (@gmail.com)"
echo "Expected: 400 Bad Request"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"999999998","email":"test@gmail.com","password":"password123","name":"테스트"}' \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "400" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 400, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 3: 필수 필드 누락 (400 Bad Request 예상)
echo "Test 3: 필수 필드 누락 (이메일 없음)"
echo "Expected: 400 Bad Request"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"999999997","password":"password123","name":"테스트"}' \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "400" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 400, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 4: 비밀번호 길이 부족 (400 Bad Request 예상)
echo "Test 4: 비밀번호 길이 부족 (7자)"
echo "Expected: 400 Bad Request"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"999999996","email":"short@inu.ac.kr","password":"pass123","name":"테스트"}' \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "400" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 400, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 5: 잘못된 비밀번호 로그인 (401 Unauthorized 예상)
echo "Test 5: 잘못된 비밀번호 로그인"
echo "Expected: 401 Unauthorized"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@inu.ac.kr","password":"wrongpassword"}' \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "401" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 401, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 6: 정상 회원가입 (201 Created 예상)
RANDOM_NUM=$RANDOM
echo "Test 6: 정상 회원가입"
echo "Expected: 201 Created"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"studentId\":\"9999${RANDOM_NUM}\",\"email\":\"newuser${RANDOM_NUM}@inu.ac.kr\",\"password\":\"password123\",\"name\":\"신규사용자\"}" \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "201" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 201, Got $HTTP_CODE)"
fi
echo ""
sleep 1

# Test 7: 정상 로그인 (200 OK 예상)
echo "Test 7: 정상 로그인"
echo "Expected: 200 OK"
RESULT=$(curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"newuser${RANDOM_NUM}@inu.ac.kr\",\"password\":\"password123\"}" \
  -s -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESULT" | grep -o "HTTP:[0-9]*" | cut -d: -f2)
MESSAGE=$(echo "$RESULT" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)

echo "HTTP Code: $HTTP_CODE"
echo "Message: $MESSAGE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (Expected 200, Got $HTTP_CODE)"
fi
echo ""

echo "========================================"
echo "           테스트 완료"
echo "========================================"
