import os
from typing import Optional, Dict, Any
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from functools import lru_cache
import logging

logger = logging.getLogger(__name__)


class DatabaseSettings(BaseSettings):
    """데이터베이스 설정"""

    url: str = Field(
        default="postgresql://postgres:postgres@host.docker.internal:5432/incheon_notice",
        alias="DATABASE_URL",
    )
    pool_size: int = Field(default=10, alias="DB_POOL_SIZE")
    max_overflow: int = Field(default=20, alias="DB_MAX_OVERFLOW")
    pool_pre_ping: bool = Field(default=True, alias="DB_POOL_PRE_PING")
    pool_recycle: int = Field(default=3600, alias="DB_POOL_RECYCLE")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class RedisSettings(BaseSettings):
    """Redis 설정"""

    broker_url: str = Field(default="redis://host.docker.internal:6379/0", alias="CELERY_BROKER_URL")
    result_backend: str = Field(
        default="redis://host.docker.internal:6379/0", alias="CELERY_RESULT_BACKEND"
    )
    host: str = Field(default="host.docker.internal", alias="REDIS_HOST")
    port: int = Field(default=6379, alias="REDIS_PORT")
    db: int = Field(default=0, alias="REDIS_DB")
    password: Optional[str] = Field(default=None, alias="REDIS_PASSWORD")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class CrawlerSettings(BaseSettings):
    """크롤러 설정"""

    default_rate_limit_per_host: float = Field(
        default=1.0, alias="DEFAULT_RATE_LIMIT_PER_HOST"
    )
    max_concurrent_requests_per_host: int = Field(
        default=2, alias="MAX_CONCURRENT_REQUESTS_PER_HOST"
    )
    browser_timeout_seconds: int = Field(default=30, alias="BROWSER_TIMEOUT_SECONDS")
    max_retries: int = Field(default=3, alias="MAX_RETRIES")
    retry_delay_seconds: int = Field(default=5, alias="RETRY_DELAY_SECONDS")
    respect_robots_txt: bool = Field(default=True, alias="RESPECT_ROBOTS_TXT")
    user_agent: str = Field(
        default="Mozilla/5.0 (compatible; CollegeNotiBot/1.0)", alias="USER_AGENT"
    )
    request_timeout: int = Field(default=30, alias="REQUEST_TIMEOUT")
    max_content_length: int = Field(
        default=10 * 1024 * 1024, alias="MAX_CONTENT_LENGTH"
    )  # 10MB

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class PlaywrightSettings(BaseSettings):
    """Playwright 설정"""

    browser_type: str = Field(default="chromium", alias="PLAYWRIGHT_BROWSER_TYPE")
    headless: bool = Field(default=True, alias="PLAYWRIGHT_HEADLESS")
    viewport_width: int = Field(default=1920, alias="PLAYWRIGHT_VIEWPORT_WIDTH")
    viewport_height: int = Field(default=1080, alias="PLAYWRIGHT_VIEWPORT_HEIGHT")
    user_agent: str = Field(
        default="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        alias="PLAYWRIGHT_USER_AGENT",
    )
    timeout: int = Field(default=30000, alias="PLAYWRIGHT_TIMEOUT")  # 30초
    wait_until: str = Field(default="networkidle", alias="PLAYWRIGHT_WAIT_UNTIL")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class MonitoringSettings(BaseSettings):
    """모니터링 설정"""

    sentry_dsn: Optional[str] = Field(default=None, alias="SENTRY_DSN")
    environment: str = Field(default="development", alias="ENV")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    enable_prometheus: bool = Field(default=True, alias="ENABLE_PROMETHEUS")
    metrics_port: int = Field(default=8000, alias="METRICS_PORT")
    health_check_interval: int = Field(default=30, alias="HEALTH_CHECK_INTERVAL")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class NotificationSettings(BaseSettings):
    """알림 설정"""

    slack_webhook_url: Optional[str] = Field(default=None, alias="SLACK_WEBHOOK_URL")
    enable_slack_notifications: bool = Field(
        default=False, alias="ENABLE_SLACK_NOTIFICATIONS"
    )
    notification_level: str = Field(default="ERROR", alias="NOTIFICATION_LEVEL")
    webhook_timeout: int = Field(default=10, alias="WEBHOOK_TIMEOUT")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class SecuritySettings(BaseSettings):
    """보안 설정"""

    # API 인증
    api_key: str = Field(default="", alias="API_KEY")
    api_key_header: str = Field(default="X-API-Key", alias="API_KEY_HEADER")

    # CORS
    allowed_origins: str = Field(
        default="http://localhost:3000,http://localhost:8000",
        alias="ALLOWED_ORIGINS"
    )

    # Trusted Hosts
    trusted_hosts: str = Field(
        default="localhost,127.0.0.1",
        alias="TRUSTED_HOSTS"
    )

    # Rate Limiting
    enable_rate_limiting: bool = Field(default=True, alias="ENABLE_RATE_LIMITING")
    max_requests_per_minute: int = Field(default=60, alias="MAX_REQUESTS_PER_MINUTE")

    # IP Filtering
    block_suspicious_ips: bool = Field(default=True, alias="BLOCK_SUSPICIOUS_IPS")

    # Proxy
    enable_proxy_rotation: bool = Field(default=False, alias="ENABLE_PROXY_ROTATION")
    proxy_list: Optional[str] = Field(default=None, alias="PROXY_LIST")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


class Settings(BaseSettings):
    """전체 설정"""

    # 기본 설정
    app_name: str = Field(default="College Notice Crawler", alias="APP_NAME")
    debug: bool = Field(default=False, alias="DEBUG")
    secret_key: str = Field(default="your-secret-key-here", alias="SECRET_KEY")

    # 하위 설정들
    database: DatabaseSettings = DatabaseSettings()
    redis: RedisSettings = RedisSettings()
    crawler: CrawlerSettings = CrawlerSettings()
    playwright: PlaywrightSettings = PlaywrightSettings()
    monitoring: MonitoringSettings = MonitoringSettings()
    notification: NotificationSettings = NotificationSettings()
    security: SecuritySettings = SecuritySettings()

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
        populate_by_name=True
    )


@lru_cache()
def get_settings() -> Settings:
    """설정 인스턴스 반환 (캐시됨)"""
    return Settings()


def get_database_url() -> str:
    """데이터베이스 URL 반환"""
    settings = get_settings()
    return settings.database.url


def get_redis_url() -> str:
    """Redis URL 반환"""
    settings = get_settings()
    return settings.redis.broker_url


def get_crawler_config() -> Dict[str, Any]:
    """크롤러 설정 반환"""
    settings = get_settings()
    return {
        "rate_limit_per_host": settings.crawler.default_rate_limit_per_host,
        "max_concurrent_requests": settings.crawler.max_concurrent_requests_per_host,
        "browser_timeout": settings.crawler.browser_timeout_seconds,
        "max_retries": settings.crawler.max_retries,
        "retry_delay": settings.crawler.retry_delay_seconds,
        "respect_robots_txt": settings.crawler.respect_robots_txt,
        "user_agent": settings.crawler.user_agent,
        "request_timeout": settings.crawler.request_timeout,
        "max_content_length": settings.crawler.max_content_length,
    }


def get_playwright_config() -> Dict[str, Any]:
    """Playwright 설정 반환"""
    settings = get_settings()
    return {
        "browser_type": settings.playwright.browser_type,
        "headless": settings.playwright.headless,
        "viewport_width": settings.playwright.viewport_width,
        "viewport_height": settings.playwright.viewport_height,
        "user_agent": settings.playwright.user_agent,
        "timeout": settings.playwright.timeout,
        "wait_until": settings.playwright.wait_until,
    }


def get_monitoring_config() -> Dict[str, Any]:
    """모니터링 설정 반환"""
    settings = get_settings()
    return {
        "sentry_dsn": settings.monitoring.sentry_dsn,
        "environment": settings.monitoring.environment,
        "log_level": settings.monitoring.log_level,
        "enable_prometheus": settings.monitoring.enable_prometheus,
        "metrics_port": settings.monitoring.metrics_port,
        "health_check_interval": settings.monitoring.health_check_interval,
    }


def get_notification_config() -> Dict[str, Any]:
    """알림 설정 반환"""
    settings = get_settings()
    return {
        "slack_webhook_url": settings.notification.slack_webhook_url,
        "enable_slack_notifications": settings.notification.enable_slack_notifications,
        "notification_level": settings.notification.notification_level,
        "webhook_timeout": settings.notification.webhook_timeout,
    }


def get_security_config() -> Dict[str, Any]:
    """보안 설정 반환"""
    settings = get_settings()
    return {
        "enable_rate_limiting": settings.security.enable_rate_limiting,
        "enable_proxy_rotation": settings.security.enable_proxy_rotation,
        "proxy_list": settings.security.proxy_list,
        "max_requests_per_minute": settings.security.max_requests_per_minute,
        "block_suspicious_ips": settings.security.block_suspicious_ips,
    }


def validate_settings():
    """설정 유효성 검사"""
    try:
        settings = get_settings()

        # 필수 설정 검사
        if not settings.database.url:
            raise ValueError("DATABASE_URL is required")

        if not settings.redis.broker_url:
            raise ValueError("CELERY_BROKER_URL is required")

        # 보안 설정 검사 (강화됨)
        # SECRET_KEY 검증
        insecure_keys = [
            "your-secret-key-here",
            "dev-secret-key",
            "test-secret-key",
            "changeme",
            "secret",
            "password",
        ]
        if not settings.secret_key or any(weak in settings.secret_key.lower() for weak in insecure_keys):
            raise ValueError(
                "❌ SECRET_KEY must be set to a secure random string. "
                "Generate one with: python -c \"import secrets; print(secrets.token_urlsafe(64))\""
            )

        if len(settings.secret_key) < 32:
            raise ValueError("❌ SECRET_KEY must be at least 32 characters long")

        # API 키 검증 (모든 환경에서 검증 강화)
        if not settings.security.api_key:
            raise ValueError(
                "❌ API_KEY is required. "
                "Generate one with: python -c \"import secrets; print(secrets.token_urlsafe(32))\""
            )

        # 프로덕션 환경 추가 검증
        if settings.monitoring.environment == "production":
            if len(settings.security.api_key) < 32:
                raise ValueError("❌ API_KEY must be at least 32 characters long in production")

            # 개발용 키 사용 방지
            if settings.security.api_key.startswith(("dev-", "test-")):
                raise ValueError(
                    "❌ Cannot use development/test API key in production! "
                    "Generate a secure key with: "
                    "python -c \"import secrets; print(secrets.token_urlsafe(32))\""
                )

            # 취약한 비밀번호 패턴 체크
            weak_passwords = [
                "crawler123",
                "password",
                "admin",
                "12345",
                "test",
                "dev",
            ]
            db_url_lower = settings.database.url.lower()
            if any(weak in db_url_lower for weak in weak_passwords):
                raise ValueError(
                    "❌ Weak database password detected in production! "
                    "Please use a strong password with at least 16 characters, "
                    "including uppercase, lowercase, numbers, and special characters."
                )

            # Redis 비밀번호 체크 (production)
            if settings.redis.password:
                if len(settings.redis.password) < 16:
                    logger.warning(
                        "⚠️  Redis password is too short for production. "
                        "Recommended: at least 16 characters."
                    )
        else:
            # 개발 환경에서도 최소한의 검증
            if len(settings.security.api_key) < 16:
                logger.warning(
                    "⚠️  API_KEY is too short even for development. "
                    "Recommended: at least 16 characters."
                )

        # 크롤러 설정 검사
        if settings.crawler.default_rate_limit_per_host <= 0:
            raise ValueError("DEFAULT_RATE_LIMIT_PER_HOST must be positive")

        if settings.crawler.max_concurrent_requests_per_host <= 0:
            raise ValueError("MAX_CONCURRENT_REQUESTS_PER_HOST must be positive")

        # Playwright 설정 검사
        if settings.playwright.browser_type not in ["chromium", "firefox", "webkit"]:
            raise ValueError(
                "PLAYWRIGHT_BROWSER_TYPE must be one of: chromium, firefox, webkit"
            )

        logger.info("✅ Settings validation passed")
        return True

    except Exception as e:
        logger.error(f"❌ Settings validation failed: {e}")
        raise


def reload_settings():
    """설정 재로드 (캐시 무효화)"""
    get_settings.cache_clear()
    logger.info("Settings cache cleared, will reload on next access")
