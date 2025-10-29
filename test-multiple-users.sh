#!/bin/bash

# 여러 사용자 등록 테스트

BASE_URL="http://localhost:8080"

echo "여러 사용자 등록 테스트 시작..."
echo ""

# 사용자 1
curl -s -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202411111",
    "email": "user1@inu.ac.kr",
    "password": "password123",
    "name": "김철수"
  }' | jq '.'

echo ""

# 사용자 2
curl -s -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202422222",
    "email": "user2@inu.ac.kr",
    "password": "password123",
    "name": "이영희"
  }' | jq '.'

echo ""

# 사용자 3
curl -s -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202433333",
    "email": "user3@inu.ac.kr",
    "password": "password123",
    "name": "박민수"
  }' | jq '.'

echo ""
echo "등록 완료! 데이터베이스 확인:"
docker exec incheon-notice-db psql -U postgres -d incheon_notice -c "SELECT id, student_id, email, name FROM users ORDER BY id;"
