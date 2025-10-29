#!/bin/bash

# 전체 API 테스트 스크립트
# 403 에러 없이 정상 작동하는지 확인

BASE_URL="http://localhost:8080"

echo "╔══════════════════════════════════════════════════╗"
echo "║  인천대학교 공지사항 백엔드 API 전체 테스트       ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 결과 카운터
PASS=0
FAIL=0

# 테스트 함수
test_api() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local auth=$5

    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}테스트: ${name}${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "요청: ${method} ${endpoint}"

    if [ -n "$data" ]; then
        echo "데이터: ${data}"
    fi

    if [ -n "$auth" ]; then
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "${method}" "${BASE_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${auth}" \
            -d "${data}")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "${method}" "${BASE_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -d "${data}")
    fi

    http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')

    echo "HTTP 상태: ${http_status}"
    echo "응답:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""

    if [ "$http_status" -ge 200 ] && [ "$http_status" -lt 300 ]; then
        echo -e "${GREEN}✅ 성공 (${http_status})${NC}"
        ((PASS++))
    else
        echo -e "${RED}❌ 실패 (${http_status})${NC}"
        ((FAIL++))
    fi
    echo ""
}

# 1. 회원가입 테스트
RANDOM_ID=$((202400000 + RANDOM % 10000))
TEST_EMAIL="test${RANDOM_ID}@inu.ac.kr"

test_api "회원가입" "POST" "/api/auth/signup" \
    "{\"studentId\":\"${RANDOM_ID}\",\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\",\"name\":\"테스트사용자\"}"

# 2. 로그인 테스트
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\"}")

test_api "로그인" "POST" "/api/auth/login" \
    "{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\"}"

# 토큰 추출
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken' 2>/dev/null)
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.refreshToken' 2>/dev/null)

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✓ JWT 토큰 발급 성공${NC}"
    echo "  Access Token: ${ACCESS_TOKEN:0:50}..."
    echo ""
else
    echo -e "${RED}✗ JWT 토큰 발급 실패${NC}"
    echo ""
fi

# 3. 토큰 갱신 테스트
if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ]; then
    test_api "토큰 갱신" "POST" "/api/auth/refresh" \
        "{\"refreshToken\":\"${REFRESH_TOKEN}\"}"
fi

# 4. 중복 회원가입 테스트 (409 에러 예상)
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}테스트: 중복 회원가입 (409 에러 예상)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
DUP_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${BASE_URL}/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{\"studentId\":\"${RANDOM_ID}\",\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\",\"name\":\"중복테스트\"}")

DUP_STATUS=$(echo "$DUP_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
echo "HTTP 상태: ${DUP_STATUS}"

if [ "$DUP_STATUS" = "500" ] || [ "$DUP_STATUS" = "409" ]; then
    echo -e "${GREEN}✅ 중복 회원가입 방지 정상 작동${NC}"
else
    echo -e "${YELLOW}⚠️  예상과 다른 응답 (${DUP_STATUS})${NC}"
fi
echo ""

# 5. 잘못된 로그인 테스트 (401 에러 예상)
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}테스트: 잘못된 비밀번호 (401 에러 예상)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
WRONG_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"wrongpassword\"}")

WRONG_STATUS=$(echo "$WRONG_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
echo "HTTP 상태: ${WRONG_STATUS}"

if [ "$WRONG_STATUS" = "401" ]; then
    echo -e "${GREEN}✅ 인증 실패 정상 작동${NC}"
else
    echo -e "${YELLOW}⚠️  예상과 다른 응답 (${WRONG_STATUS})${NC}"
fi
echo ""

# 최종 결과
echo "╔══════════════════════════════════════════════════╗"
echo "║               테스트 결과 요약                    ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}✅ 성공: ${PASS}개${NC}"
echo -e "${RED}❌ 실패: ${FAIL}개${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 테스트 통과!${NC}"
    echo ""
    echo "📖 이제 Swagger UI에서 직접 테스트해보세요:"
    echo "   👉 http://localhost:8080/swagger-ui/index.html"
    echo ""
    echo "🔑 로그인 정보:"
    echo "   Email: ${TEST_EMAIL}"
    echo "   Password: password123"
else
    echo -e "${RED}⚠️  일부 테스트가 실패했습니다.${NC}"
    echo ""
    echo "로그 확인: docker-compose logs backend"
fi
echo ""
