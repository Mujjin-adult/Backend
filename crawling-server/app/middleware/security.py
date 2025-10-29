from fastapi import Request, Depends
from fastapi import Request, HTTPException
from fastapi.security import APIKeyHeader
from typing import List, Optional
import time
from collections import defaultdict
import os

# API 키 검증
api_key_header = APIKeyHeader(name="X-API-Key")

async def verify_api_key(api_key: str = Depends(api_key_header)):
    if api_key != os.getenv("API_KEY"):
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
        self.last_cleanup = time.time()

    async def check_rate_limit(self, client_ip: str) -> bool:
        now = time.time()
        minute_ago = now - 60

        # 주기적으로 오래된 IP 항목 정리 (10분마다)
        if now - self.last_cleanup > 600:
            self._cleanup_old_ips(now)
            self.last_cleanup = now

        # 1분 이전의 요청 제거
        self.requests[client_ip] = [
            req_time for req_time in self.requests[client_ip]
            if req_time > minute_ago
        ]

        # 빈 항목 제거
        if not self.requests[client_ip]:
            del self.requests[client_ip]
            return True

        # 요청 수 확인
        if len(self.requests[client_ip]) >= self.requests_per_minute:
            return False

        self.requests[client_ip].append(now)
        return True

    def _cleanup_old_ips(self, now: float):
        """5분 이상 요청이 없는 IP 제거 (메모리 누수 방지)"""
        five_minutes_ago = now - 300
        ips_to_remove = [
            ip for ip, times in self.requests.items()
            if not times or max(times) < five_minutes_ago
        ]
        for ip in ips_to_remove:
            del self.requests[ip]

# 보안 헤더 미들웨어
async def add_security_headers(request: Request, call_next):
    response = await call_next(request)

    # 보안 헤더 추가
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"

    # Swagger UI(/docs)와 OpenAPI JSON(/openapi.json)은 CDN 리소스 허용
    if request.url.path in ["/docs", "/openapi.json", "/redoc"]:
        response.headers["Content-Security-Policy"] = (
            "default-src 'self'; "
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
            "img-src 'self' data: https://fastapi.tiangolo.com; "
            "font-src 'self' https://cdn.jsdelivr.net;"
        )
    else:
        # 다른 경로는 엄격한 CSP 유지
        response.headers["Content-Security-Policy"] = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"

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
