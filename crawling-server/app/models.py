from sqlalchemy import (
    Column,
    Integer,
    String,
    Text,
    Boolean,
    Float,
    DateTime,
    JSON,
    ForeignKey,
    Enum,
)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum

Base = declarative_base()


class SeedType(str, enum.Enum):
    URL_LIST = "URL_LIST"
    SITEMAP = "SITEMAP"
    DOMAIN = "DOMAIN"
    SEARCH = "SEARCH"


class RenderMode(str, enum.Enum):
    STATIC = "STATIC"
    HEADLESS = "HEADLESS"
    AUTO = "AUTO"


class RobotsPolicy(str, enum.Enum):
    OBEY = "OBEY"
    IGNORE_WHITELIST = "IGNORE_WHITELIST"


class WebhookEvent(str, enum.Enum):
    JOB_DONE = "JOB_DONE"
    DOC_READY = "DOC_READY"
    ERROR = "ERROR"


class TaskStatus(str, enum.Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    RETRY = "RETRY"
    BLOCKED = "BLOCKED"


class JobStatus(str, enum.Enum):
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ERROR = "ERROR"


class CrawlJob(Base):
    __tablename__ = "crawl_job"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(128), nullable=False, index=True)
    priority = Column(String(4), nullable=False, index=True)  # P0, P1, P2, P3
    schedule_cron = Column(String(64), nullable=True)
    seed_type = Column(Enum(SeedType), nullable=False)
    seed_payload = Column(JSON, nullable=False)
    render_mode = Column(Enum(RenderMode), nullable=False)
    rate_limit_per_host = Column(Float, default=1.0)
    max_depth = Column(Integer, default=1)
    robots_policy = Column(Enum(RobotsPolicy), nullable=False)
    status = Column(Enum(JobStatus), default=JobStatus.ACTIVE, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    tasks = relationship("CrawlTask", back_populates="job")
    documents = relationship("CrawlNotice", back_populates="job")
    webhooks = relationship("Webhook", back_populates="job")


class CrawlTask(Base):
    __tablename__ = "crawl_task"

    id = Column(Integer, primary_key=True, index=True)
    job_id = Column(Integer, ForeignKey("crawl_job.id"), nullable=False, index=True)
    url = Column(Text, nullable=False, index=True)
    status = Column(Enum(TaskStatus), default=TaskStatus.PENDING, index=True)
    retries = Column(Integer, default=0)
    last_error = Column(Text, nullable=True)
    started_at = Column(DateTime(timezone=True), nullable=True)
    finished_at = Column(DateTime(timezone=True), nullable=True)
    http_status = Column(Integer, nullable=True)
    content_hash = Column(String(64), nullable=True, index=True)
    blocked_flag = Column(Boolean, default=False, index=True)
    cost_ms_browser = Column(Integer, nullable=True)
    attempt_strategy = Column(JSON, nullable=True)

    # Relationships
    job = relationship("CrawlJob", back_populates="tasks")


class CrawlNotice(Base):
    __tablename__ = "crawl_notice"

    id = Column(Integer, primary_key=True, index=True)
    job_id = Column(Integer, ForeignKey("crawl_job.id"), nullable=False, index=True)
    url = Column(Text, nullable=False, index=True)

    # 크롤링된 필드들
    title = Column(Text, nullable=True)
    writer = Column(String(128), nullable=True)
    date = Column(String(32), nullable=True)
    hits = Column(String(32), nullable=True)
    category = Column(String(64), nullable=True)
    source = Column(String(64), nullable=True)

    # 메인 서버 통합을 위한 추가 필드들
    content = Column(Text, nullable=True)  # 공지사항 본문 내용
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())  # 업데이트 시간

    # 원본 데이터 (deprecated - 하위 호환성 유지)
    extracted = Column(JSON, nullable=True)
    raw = Column(Text, nullable=True)

    snapshot_version = Column(String(32), nullable=True)
    fingerprint = Column(String(64), nullable=False, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    job = relationship("CrawlJob", back_populates="documents")


class HostBudget(Base):
    __tablename__ = "host_budget"

    host = Column(String(128), primary_key=True, index=True)
    qps_limit = Column(Float, default=1.0)
    concurrency_limit = Column(Integer, default=1)
    daily_budget_sec_browser = Column(Integer, default=3600)  # 1시간
    used_today_sec = Column(Integer, default=0)


class Webhook(Base):
    __tablename__ = "webhook"

    id = Column(Integer, primary_key=True, index=True)
    job_id = Column(Integer, ForeignKey("crawl_job.id"), nullable=False, index=True)
    target_url = Column(Text, nullable=False)
    event = Column(Enum(WebhookEvent), nullable=False)
    secret = Column(String(128), nullable=True)

    # Relationships
    job = relationship("CrawlJob", back_populates="webhooks")


class DetailCategory(Base):
    """상세 카테고리 (공지사항의 category 필드 값 관리)"""
    __tablename__ = "detail_category"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, unique=True, index=True)
    description = Column(String(255), nullable=True)
    is_active = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
