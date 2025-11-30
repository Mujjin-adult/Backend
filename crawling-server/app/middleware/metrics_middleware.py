"""
Metrics 미들웨어

모든 HTTP 요청에 대해 자동으로 메트릭을 기록합니다.
"""
import time
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
import logging

logger = logging.getLogger(__name__)


class MetricsMiddleware(BaseHTTPMiddleware):
    """HTTP 요청 메트릭을 자동으로 기록하는 미들웨어"""

    async def dispatch(self, request: Request, call_next):
        """
        요청을 처리하고 메트릭을 기록합니다.

        Args:
            request: FastAPI Request 객체
            call_next: 다음 미들웨어 또는 라우트 핸들러

        Returns:
            Response 객체
        """
        # 시작 시간 기록
        start_time = time.time()

        # 요청 처리
        response = await call_next(request)

        # 소요 시간 계산
        duration = time.time() - start_time

        # 메트릭 기록 (지연 import로 중복 등록 방지)
        try:
            from metrics import record_http_request
            record_http_request(
                method=request.method,
                endpoint=request.url.path,
                status=response.status_code,
                duration=duration
            )
        except Exception as e:
            logger.error(f"Failed to record HTTP metrics: {e}")

        return response
