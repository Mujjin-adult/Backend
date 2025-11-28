"""
Pydantic 스키마 정의

데이터 검증 및 직렬화를 위한 스키마 클래스들
"""
from pydantic import BaseModel, Field, HttpUrl, validator, root_validator
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum


# ===================================================================
# Enums
# ===================================================================

class CrawlSource(str, Enum):
    """크롤링 소스"""
    VOLUNTEER = "volunteer"
    JOB = "job"
    SCHOLARSHIP = "scholarship"
    GENERAL_EVENTS = "general_events"
    EDUCATIONAL_TEST = "educational_test"
    TUITION_PAYMENT = "tuition_payment"
    ACADEMIC_CREDIT = "academic_credit"
    DEGREE = "degree"


class TaskStatus(str, Enum):
    """태스크 상태"""
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    RETRY = "RETRY"
    BLOCKED = "BLOCKED"


class JobStatus(str, Enum):
    """작업 상태"""
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ERROR = "ERROR"


# ===================================================================
# Notice Schemas (크롤링된 공지사항)
# ===================================================================

class NoticeBase(BaseModel):
    """공지사항 기본 스키마"""
    title: str = Field(..., min_length=1, max_length=500, description="공지사항 제목")
    url: HttpUrl = Field(..., description="공지사항 URL")
    writer: Optional[str] = Field(None, max_length=128, description="작성자")
    date: Optional[str] = Field(None, max_length=32, description="작성일자 (예: 2025.11.02)")
    hits: Optional[str] = Field(None, max_length=32, description="조회수")
    category: Optional[str] = Field(None, max_length=64, description="카테고리")
    source: CrawlSource = Field(..., description="크롤링 소스")

    @validator('title')
    def title_must_not_be_empty(cls, v):
        """제목은 공백만 있으면 안됨"""
        if not v or v.strip() == "":
            raise ValueError('Title must not be empty or whitespace only')
        return v.strip()

    @validator('date')
    def validate_date_format(cls, v):
        """날짜 형식 검증 (선택적)"""
        if v is None:
            return v
        # 기본적인 형식 체크 (YYYY.MM.DD 또는 YYYY-MM-DD)
        if v and len(v) >= 8:
            return v
        return None

    class Config:
        use_enum_values = True


class NoticeCreate(NoticeBase):
    """공지사항 생성 스키마"""
    job_id: int = Field(..., gt=0, description="관련 작업 ID")
    fingerprint: str = Field(..., min_length=32, max_length=64, description="중복 체크용 해시값")
    snapshot_version: str = Field(default="v1", max_length=32, description="스냅샷 버전")
    extracted: Optional[Dict[str, Any]] = Field(default=None, description="추출된 원본 데이터")
    raw: Optional[str] = Field(default=None, description="원본 JSON 문자열")


class NoticeResponse(NoticeBase):
    """공지사항 응답 스키마"""
    id: int
    job_id: int
    fingerprint: str
    snapshot_version: str
    created_at: datetime

    class Config:
        from_attributes = True  # Pydantic v2


class NoticeListResponse(BaseModel):
    """공지사항 리스트 응답"""
    total: int = Field(..., description="전체 개수")
    items: List[NoticeResponse] = Field(..., description="공지사항 목록")
    page: int = Field(default=1, ge=1, description="현재 페이지")
    size: int = Field(default=20, ge=1, le=100, description="페이지 크기")


# ===================================================================
# Crawl Job Schemas (크롤링 작업)
# ===================================================================

class CrawlJobBase(BaseModel):
    """크롤링 작업 기본 스키마"""
    name: str = Field(..., min_length=1, max_length=128, description="작업 이름")
    priority: str = Field(..., pattern=r'^P[0-3]$', description="우선순위 (P0~P3)")
    schedule_cron: Optional[str] = Field(None, max_length=64, description="크론 스케줄")

    @validator('priority')
    def validate_priority(cls, v):
        """우선순위 검증"""
        if v not in ['P0', 'P1', 'P2', 'P3']:
            raise ValueError('Priority must be P0, P1, P2, or P3')
        return v


class CrawlJobCreate(CrawlJobBase):
    """크롤링 작업 생성 스키마"""
    seed_type: str = Field(..., description="시드 타입")
    seed_payload: Dict[str, Any] = Field(..., description="시드 페이로드")
    render_mode: str = Field(..., description="렌더 모드")
    rate_limit_per_host: float = Field(default=1.0, gt=0, description="호스트당 요청 제한")
    max_depth: int = Field(default=1, ge=1, description="최대 크롤링 깊이")
    robots_policy: str = Field(..., description="Robots.txt 정책")


class CrawlJobResponse(CrawlJobBase):
    """크롤링 작업 응답 스키마"""
    id: int
    status: JobStatus
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
        use_enum_values = True


# ===================================================================
# Crawl Task Schemas (크롤링 태스크)
# ===================================================================

class CrawlTaskBase(BaseModel):
    """크롤링 태스크 기본 스키마"""
    job_id: int = Field(..., gt=0, description="작업 ID")
    url: HttpUrl = Field(..., description="크롤링할 URL")


class CrawlTaskCreate(CrawlTaskBase):
    """크롤링 태스크 생성 스키마"""
    status: TaskStatus = Field(default=TaskStatus.PENDING, description="태스크 상태")


class CrawlTaskResponse(CrawlTaskBase):
    """크롤링 태스크 응답 스키마"""
    id: int
    status: TaskStatus
    retries: int
    last_error: Optional[str]
    started_at: Optional[datetime]
    finished_at: Optional[datetime]
    http_status: Optional[int]

    class Config:
        from_attributes = True
        use_enum_values = True


# ===================================================================
# API Request/Response Schemas
# ===================================================================

class CrawlerTriggerRequest(BaseModel):
    """크롤러 수동 실행 요청"""
    max_pages: Optional[int] = Field(default=5, ge=1, le=20, description="최대 페이지 수")

    @validator('max_pages')
    def validate_max_pages(cls, v):
        """최대 페이지 수 검증"""
        if v and v > 20:
            raise ValueError('max_pages must not exceed 20')
        return v


class CrawlerTriggerResponse(BaseModel):
    """크롤러 수동 실행 응답"""
    status: str = Field(..., description="실행 상태")
    category: str = Field(..., description="크롤링 카테고리")
    message: str = Field(..., description="메시지")
    tasks: List[Dict[str, Any]] = Field(default_factory=list, description="생성된 태스크 목록")
    count: int = Field(..., ge=0, description="생성된 태스크 수")


class CrawlStatsResponse(BaseModel):
    """크롤링 통계 응답"""
    total_jobs: int = Field(..., description="전체 작업 수")
    active_jobs: int = Field(..., description="활성 작업 수")
    total_tasks: int = Field(..., description="전체 태스크 수")
    successful_tasks: int = Field(..., description="성공한 태스크 수")
    failed_tasks: int = Field(..., description="실패한 태스크 수")
    total_notices: int = Field(..., description="수집된 공지사항 수")
    notices_by_source: Dict[str, int] = Field(default_factory=dict, description="소스별 공지사항 수")


class HealthCheckResponse(BaseModel):
    """헬스 체크 응답"""
    status: str = Field(..., description="서비스 상태")
    timestamp: datetime = Field(default_factory=datetime.now, description="체크 시간")
    version: str = Field(default="1.0.0", description="서비스 버전")
    database: bool = Field(..., description="데이터베이스 연결 상태")
    redis: bool = Field(..., description="Redis 연결 상태")
    celery_workers: int = Field(..., ge=0, description="활성 Celery 워커 수")


# ===================================================================
# Circuit Breaker Schemas
# ===================================================================

class CircuitBreakerStats(BaseModel):
    """Circuit Breaker 통계"""
    name: str = Field(..., description="Circuit Breaker 이름")
    state: str = Field(..., description="현재 상태 (CLOSED/OPEN/HALF_OPEN)")
    failure_count: int = Field(..., ge=0, description="실패 횟수")
    success_count: int = Field(..., ge=0, description="성공 횟수")
    total_calls: int = Field(..., ge=0, description="전체 호출 수")
    success_rate: float = Field(..., ge=0, le=100, description="성공률 (%)")
    avg_duration: float = Field(..., ge=0, description="평균 응답 시간 (초)")
    last_failure_time: Optional[float] = Field(None, description="마지막 실패 시간 (timestamp)")


class CircuitBreakerStatsResponse(BaseModel):
    """Circuit Breaker 통계 응답"""
    circuit_breakers: Dict[str, CircuitBreakerStats] = Field(..., description="Circuit Breaker 통계")


# ===================================================================
# Error Response Schemas
# ===================================================================

class ErrorDetail(BaseModel):
    """에러 상세 정보"""
    field: Optional[str] = Field(None, description="에러 발생 필드")
    message: str = Field(..., description="에러 메시지")
    type: Optional[str] = Field(None, description="에러 타입")


class ErrorResponse(BaseModel):
    """에러 응답"""
    detail: str = Field(..., description="에러 메시지")
    errors: Optional[List[ErrorDetail]] = Field(None, description="상세 에러 목록")
    timestamp: datetime = Field(default_factory=datetime.now, description="에러 발생 시간")


# ===================================================================
# Validation Utilities
# ===================================================================

def validate_notice_data(data: Dict[str, Any]) -> NoticeCreate:
    """
    공지사항 데이터 검증

    Args:
        data: 검증할 데이터

    Returns:
        검증된 NoticeCreate 객체

    Raises:
        ValidationError: 검증 실패 시
    """
    return NoticeCreate(**data)


def validate_crawler_trigger(data: Dict[str, Any]) -> CrawlerTriggerRequest:
    """
    크롤러 트리거 요청 검증

    Args:
        data: 검증할 데이터

    Returns:
        검증된 CrawlerTriggerRequest 객체

    Raises:
        ValidationError: 검증 실패 시
    """
    return CrawlerTriggerRequest(**data)


# ===================================================================
# Content Crawling Schemas (본문 크롤링)
# ===================================================================

class ContentCrawlRequest(BaseModel):
    """본문 크롤링 요청"""
    limit: int = Field(100, description="크롤링할 최대 공지사항 수", ge=1, le=500)
    source: Optional[CrawlSource] = Field(None, description="카테고리 필터")

    class Config:
        schema_extra = {
            "example": {
                "limit": 100,
                "source": "volunteer"
            }
        }


class ContentCrawlErrorDetail(BaseModel):
    """본문 크롤링 에러 상세"""
    notice_id: int = Field(..., description="공지사항 ID")
    url: str = Field(..., description="크롤링 실패한 URL")
    error: str = Field(..., description="에러 메시지")


class ContentCrawlResult(BaseModel):
    """본문 크롤링 결과"""
    total_attempted: int = Field(..., description="크롤링 시도한 총 개수")
    success_count: int = Field(..., description="성공한 개수")
    failed_count: int = Field(..., description="실패한 개수")
    failed_notices: List[ContentCrawlErrorDetail] = Field(
        default_factory=list,
        description="실패한 공지사항 목록"
    )
    execution_time_seconds: float = Field(..., description="실행 시간 (초)")

    class Config:
        schema_extra = {
            "example": {
                "total_attempted": 50,
                "success_count": 45,
                "failed_count": 5,
                "failed_notices": [
                    {
                        "notice_id": 123,
                        "url": "https://www.inu.ac.kr/bbs/inu/253/artclView.do?artclSeq=123",
                        "error": "Request timeout"
                    }
                ],
                "execution_time_seconds": 125.5
            }
        }
