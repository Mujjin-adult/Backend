"""
Sentry 에러 추적 설정

실시간 에러 모니터링 및 성능 추적을 위한 Sentry 설정
"""
import os
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration
from sentry_sdk.integrations.sqlalchemy import SqlalchemyIntegration
from sentry_sdk.integrations.redis import RedisIntegration
from sentry_sdk.integrations.celery import CeleryIntegration
from sentry_sdk.integrations.logging import LoggingIntegration
import logging

logger = logging.getLogger(__name__)


def init_sentry():
    """
    Sentry SDK 초기화

    환경 변수:
        SENTRY_DSN: Sentry DSN (비어있으면 Sentry 비활성화)
        ENV: 환경 (development, staging, production)
        APP_NAME: 애플리케이션 이름

    Returns:
        bool: 초기화 성공 여부
    """
    sentry_dsn = os.getenv("SENTRY_DSN", "").strip()

    # DSN이 없으면 Sentry 비활성화
    if not sentry_dsn:
        logger.info("Sentry DSN not configured, error tracking disabled")
        return False

    try:
        environment = os.getenv("ENV", "development")
        app_name = os.getenv("APP_NAME", "College Notice Crawler")

        # Sentry 초기화
        sentry_sdk.init(
            dsn=sentry_dsn,

            # 환경 설정
            environment=environment,
            release=f"{app_name}@1.0.0",

            # 통합 설정
            integrations=[
                # FastAPI 통합
                FastApiIntegration(
                    transaction_style="endpoint",  # 엔드포인트별 트랜잭션
                ),

                # SQLAlchemy 통합 (데이터베이스 쿼리 추적)
                SqlalchemyIntegration(),

                # Redis 통합
                RedisIntegration(),

                # Celery 통합 (백그라운드 작업 추적)
                CeleryIntegration(
                    monitor_beat_tasks=True,  # Beat 작업 모니터링
                ),

                # 로깅 통합
                LoggingIntegration(
                    level=logging.INFO,  # INFO 레벨 이상 캡처
                    event_level=logging.ERROR,  # ERROR 레벨 이상 이벤트로 전송
                ),
            ],

            # 성능 모니터링 설정
            traces_sample_rate=1.0 if environment == "development" else 0.1,

            # 프로파일링 설정
            profiles_sample_rate=1.0 if environment == "development" else 0.1,

            # 에러 샘플링 (모든 에러 전송)
            sample_rate=1.0,

            # 추가 옵션
            attach_stacktrace=True,  # 스택 트레이스 자동 첨부
            send_default_pii=False,  # 개인정보 전송 비활성화

            # 제외할 에러들
            ignore_errors=[
                KeyboardInterrupt,
                SystemExit,
            ],

            # 요청 정보 추가
            before_send=before_send_handler,
        )

        logger.info(f"Sentry initialized successfully (environment: {environment})")
        return True

    except Exception as e:
        logger.error(f"Failed to initialize Sentry: {e}")
        return False


def before_send_handler(event, hint):
    """
    이벤트 전송 전 핸들러

    민감한 정보 제거 및 추가 컨텍스트 추가

    Args:
        event: Sentry 이벤트
        hint: 힌트 정보

    Returns:
        수정된 이벤트 또는 None (전송 안함)
    """
    # 민감한 헤더 제거
    if "request" in event:
        headers = event["request"].get("headers", {})
        sensitive_headers = ["authorization", "x-api-key", "cookie", "set-cookie"]

        for header in sensitive_headers:
            if header in headers:
                headers[header] = "[Filtered]"

    # 환경 변수에서 민감한 정보 제거
    if "extra" in event and "sys.argv" in event["extra"]:
        del event["extra"]["sys.argv"]

    return event


def capture_exception_with_context(exception: Exception, context: dict = None):
    """
    컨텍스트와 함께 예외 캡처

    Args:
        exception: 캡처할 예외
        context: 추가 컨텍스트 정보
    """
    if context:
        with sentry_sdk.push_scope() as scope:
            for key, value in context.items():
                scope.set_extra(key, value)
            sentry_sdk.capture_exception(exception)
    else:
        sentry_sdk.capture_exception(exception)


def capture_message_with_level(message: str, level: str = "info", context: dict = None):
    """
    레벨과 컨텍스트와 함께 메시지 캡처

    Args:
        message: 캡처할 메시지
        level: 로그 레벨 (debug, info, warning, error, fatal)
        context: 추가 컨텍스트 정보
    """
    if context:
        with sentry_sdk.push_scope() as scope:
            for key, value in context.items():
                scope.set_extra(key, value)
            sentry_sdk.capture_message(message, level=level)
    else:
        sentry_sdk.capture_message(message, level=level)


def set_user_context(user_id: str = None, email: str = None, username: str = None):
    """
    사용자 컨텍스트 설정

    Args:
        user_id: 사용자 ID
        email: 이메일
        username: 사용자명
    """
    sentry_sdk.set_user({
        "id": user_id,
        "email": email,
        "username": username,
    })


def set_tag(key: str, value: str):
    """
    태그 설정 (필터링 및 검색용)

    Args:
        key: 태그 키
        value: 태그 값
    """
    sentry_sdk.set_tag(key, value)


def set_context(name: str, context: dict):
    """
    컨텍스트 설정

    Args:
        name: 컨텍스트 이름
        context: 컨텍스트 데이터
    """
    sentry_sdk.set_context(name, context)


# 크롤러 에러 추적 헬퍼
def track_crawler_error(
    category: str,
    error_type: str,
    url: str = None,
    exception: Exception = None,
    extra_data: dict = None
):
    """
    크롤러 에러 추적

    Args:
        category: 크롤링 카테고리 (volunteer, job, etc.)
        error_type: 에러 유형 (TemporaryError, PermanentError, etc.)
        url: 에러가 발생한 URL
        exception: 예외 객체
        extra_data: 추가 데이터
    """
    with sentry_sdk.push_scope() as scope:
        # 태그 설정
        scope.set_tag("crawler.category", category)
        scope.set_tag("crawler.error_type", error_type)

        # 컨텍스트 설정
        crawler_context = {
            "category": category,
            "error_type": error_type,
        }

        if url:
            crawler_context["url"] = url

        if extra_data:
            crawler_context.update(extra_data)

        scope.set_context("crawler", crawler_context)

        # 예외 또는 메시지 캡처
        if exception:
            sentry_sdk.capture_exception(exception)
        else:
            sentry_sdk.capture_message(
                f"Crawler error: {error_type} in {category}",
                level="error"
            )
