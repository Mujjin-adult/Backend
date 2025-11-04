from fastapi import Request, HTTPException, Depends
from fastapi.security import APIKeyHeader
from typing import List, Optional
import time
import hashlib
import hmac
import secrets
from collections import defaultdict
import logging

from app.config import get_settings

logger = logging.getLogger(__name__)

# API 키 검증
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


async def verify_api_key(api_key: Optional[str] = Depends(api_key_header)):
    """
    API 키 검증 의존성

    - 타이밍 공격 방지를 위한 constant-time 비교 사용
    - 잘못된 API 키 시도 로깅
    """
    settings = get_settings()
    expected_key = settings.security.api_key

    if not api_key:
        logger.warning("API request without API key")
        raise HTTPException(
            status_code=401,
            detail="API key is required",
            headers={"WWW-Authenticate": "ApiKey"}
        )

    # Constant-time comparison to prevent timing attacks
    if not secrets.compare_digest(api_key, expected_key):
        logger.warning(f"Invalid API key attempt: {api_key[:8]}...")
        raise HTTPException(
            status_code=403,
            detail="Invalid API key"
        )

    return api_key

# 레이트 리미팅
class RateLimiter:
    def __init__(self, requests_per_minute: int = 60):
        self.requests_per_minute = requests_per_minute
        self.requests = defaultdict(list)

    async def check_rate_limit(self, client_ip: str) -> bool:
        now = time.time()
        minute_ago = now - 60

        # 1분 이전의 요청 제거
        self.requests[client_ip] = [
            req_time for req_time in self.requests[client_ip]
            if req_time > minute_ago
        ]

        # 요청 수 확인
        if len(self.requests[client_ip]) >= self.requests_per_minute:
            return False

        self.requests[client_ip].append(now)
        return True

# 보안 헤더 미들웨어
async def add_security_headers(request: Request, call_next):
    response = await call_next(request)

    # 보안 헤더 추가
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"

    # Swagger UI와 ReDoc을 위한 CSP 설정 (CDN 허용)
    # /docs 또는 /redoc 경로에서는 더 관대한 CSP 적용
    if request.url.path in ["/docs", "/redoc", "/openapi.json"]:
        response.headers["Content-Security-Policy"] = (
            "default-src 'self'; "
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
            "font-src 'self' https://fonts.gstatic.com; "
            "img-src 'self' data: https://cdn.jsdelivr.net https://fastapi.tiangolo.com;"
        )
    else:
        # 일반 페이지는 더 엄격한 CSP 적용
        response.headers["Content-Security-Policy"] = (
            "default-src 'self'; "
            "script-src 'self' 'unsafe-inline'; "
            "style-src 'self' 'unsafe-inline';"
        )

    return response

# IP 차단 미들웨어
class IPBlocker:
    def __init__(self):
        self.blocked_ips = set()
        self.suspicious_attempts = defaultdict(int)

    def is_ip_blocked(self, ip: str) -> bool:
        return ip in self.blocked_ips

    def record_suspicious_attempt(self, ip: str):
        self.suspicious_attempts[ip] += 1
        if self.suspicious_attempts[ip] >= 5:  # 5회 이상 의심스러운 시도
            self.blocked_ips.add(ip)

    def clear_old_attempts(self):
        # 24시간마다 기록 초기화
        self.suspicious_attempts.clear()
