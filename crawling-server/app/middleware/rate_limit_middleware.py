"""
API Rate Limiting 미들웨어

요청 속도 제한을 적용하여 API 남용을 방지합니다.
Redis를 사용한 분산 환경 지원.
"""

import logging
import time
from typing import Optional, Callable
from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
import redis
import os

logger = logging.getLogger(__name__)


class RateLimitMiddleware(BaseHTTPMiddleware):
    """
    Rate Limiting 미들웨어

    IP 주소 또는 API 키 기반으로 요청 속도를 제한합니다.
    """

    def __init__(
        self,
        app,
        redis_client: Optional[redis.Redis] = None,
        max_requests: int = 60,
        window_seconds: int = 60,
        enable: bool = True,
        exempt_paths: list = None,
    ):
        """
        Args:
            app: FastAPI 애플리케이션
            redis_client: Redis 클라이언트 (None이면 메모리 기반 사용)
            max_requests: 시간 윈도우당 최대 요청 수
            window_seconds: 시간 윈도우 (초)
            enable: Rate Limiting 활성화 여부
            exempt_paths: Rate Limiting에서 제외할 경로 리스트
        """
        super().__init__(app)
        self.redis_client = redis_client
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.enable = enable
        self.exempt_paths = exempt_paths or ["/health", "/metrics", "/docs", "/openapi.json"]

        # Redis가 없으면 메모리 기반 사용 (단일 인스턴스만 지원)
        self.memory_store = {} if not redis_client else None

        logger.info(
            f"Rate Limit Middleware initialized: "
            f"max_requests={max_requests}, window={window_seconds}s, "
            f"backend={'redis' if redis_client else 'memory'}"
        )

    async def dispatch(
        self, request: Request, call_next: Callable
    ) -> Response:
        """요청 처리 및 Rate Limiting 적용"""

        # Rate Limiting 비활성화된 경우
        if not self.enable:
            return await call_next(request)

        # 제외 경로 확인
        if any(request.url.path.startswith(path) for path in self.exempt_paths):
            return await call_next(request)

        # 클라이언트 식별자 가져오기 (API 키 또는 IP)
        client_id = self._get_client_id(request)

        # Rate Limit 확인
        allowed, remaining, reset_time = await self._check_rate_limit(client_id)

        # 헤더 추가
        response = await call_next(request) if allowed else None

        if not allowed:
            # Rate Limit 초과
            logger.warning(
                f"Rate limit exceeded for client {client_id} "
                f"(path: {request.url.path})"
            )
            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={
                    "error": "Too Many Requests",
                    "message": f"Rate limit exceeded. Try again in {reset_time} seconds.",
                    "retry_after": reset_time,
                },
                headers={
                    "X-RateLimit-Limit": str(self.max_requests),
                    "X-RateLimit-Remaining": "0",
                    "X-RateLimit-Reset": str(int(time.time()) + reset_time),
                    "Retry-After": str(reset_time),
                },
            )

        # Rate Limit 정보를 응답 헤더에 추가
        response.headers["X-RateLimit-Limit"] = str(self.max_requests)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Reset"] = str(int(time.time()) + reset_time)

        return response

    def _get_client_id(self, request: Request) -> str:
        """클라이언트 식별자 가져오기 (API 키 또는 IP 주소)"""
        # API 키 우선
        api_key = request.headers.get("X-API-Key")
        if api_key:
            return f"api_key:{api_key}"

        # IP 주소
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            # 프록시를 통한 요청인 경우 첫 번째 IP 사용
            client_ip = forwarded.split(",")[0].strip()
        else:
            client_ip = request.client.host if request.client else "unknown"

        return f"ip:{client_ip}"

    async def _check_rate_limit(self, client_id: str) -> tuple:
        """
        Rate Limit 확인

        Returns:
            tuple: (allowed: bool, remaining: int, reset_time: int)
        """
        if self.redis_client:
            return await self._check_rate_limit_redis(client_id)
        else:
            return await self._check_rate_limit_memory(client_id)

    async def _check_rate_limit_redis(self, client_id: str) -> tuple:
        """Redis 기반 Rate Limit 확인"""
        key = f"rate_limit:{client_id}"
        current_time = int(time.time())

        try:
            # Redis Pipeline 사용 (성능 최적화)
            pipe = self.redis_client.pipeline()
            pipe.zadd(key, {str(current_time): current_time})
            pipe.zremrangebyscore(
                key, 0, current_time - self.window_seconds
            )
            pipe.zcard(key)
            pipe.ttl(key)
            results = pipe.execute()

            request_count = results[2]  # zcard 결과

            # TTL이 없으면 설정
            if results[3] == -1:
                self.redis_client.expire(key, self.window_seconds)

            # Rate Limit 확인
            if request_count > self.max_requests:
                remaining = 0
                reset_time = self.window_seconds
                return False, remaining, reset_time
            else:
                remaining = self.max_requests - request_count
                reset_time = self.window_seconds
                return True, remaining, reset_time

        except Exception as e:
            logger.error(f"Redis error in rate limiting: {e}")
            # Redis 에러 시 요청 허용 (Fail-Open)
            return True, self.max_requests, self.window_seconds

    async def _check_rate_limit_memory(self, client_id: str) -> tuple:
        """메모리 기반 Rate Limit 확인 (단일 인스턴스만 지원)"""
        current_time = int(time.time())

        if client_id not in self.memory_store:
            self.memory_store[client_id] = []

        # 시간 윈도우 밖의 요청 제거
        self.memory_store[client_id] = [
            timestamp
            for timestamp in self.memory_store[client_id]
            if timestamp > current_time - self.window_seconds
        ]

        # 현재 요청 추가
        self.memory_store[client_id].append(current_time)

        request_count = len(self.memory_store[client_id])

        # Rate Limit 확인
        if request_count > self.max_requests:
            remaining = 0
            reset_time = self.window_seconds
            return False, remaining, reset_time
        else:
            remaining = self.max_requests - request_count
            reset_time = self.window_seconds
            return True, remaining, reset_time


def get_redis_client() -> Optional[redis.Redis]:
    """Redis 클라이언트 가져오기"""
    try:
        redis_host = os.getenv("REDIS_HOST", "localhost")
        redis_port = int(os.getenv("REDIS_PORT", 6379))
        redis_db = int(os.getenv("REDIS_DB", 0))
        redis_password = os.getenv("REDIS_PASSWORD")

        client = redis.Redis(
            host=redis_host,
            port=redis_port,
            db=redis_db,
            password=redis_password,
            decode_responses=True,
            socket_connect_timeout=5,
            socket_timeout=5,
        )

        # 연결 테스트
        client.ping()
        logger.info(f"Redis client connected: {redis_host}:{redis_port}")
        return client

    except Exception as e:
        logger.warning(f"Failed to connect to Redis: {e}. Using memory-based rate limiting.")
        return None


def create_rate_limit_middleware(app):
    """Rate Limit 미들웨어 생성 및 추가"""
    enable = os.getenv("ENABLE_RATE_LIMITING", "true").lower() == "true"
    max_requests = int(os.getenv("MAX_REQUESTS_PER_MINUTE", 60))
    window_seconds = 60  # 1분

    if not enable:
        logger.info("Rate limiting is disabled")
        return

    # Redis 클라이언트 가져오기
    redis_client = get_redis_client()

    # 미들웨어 추가
    app.add_middleware(
        RateLimitMiddleware,
        redis_client=redis_client,
        max_requests=max_requests,
        window_seconds=window_seconds,
        enable=enable,
    )

    logger.info(
        f"Rate limiting enabled: {max_requests} requests per {window_seconds} seconds"
    )
