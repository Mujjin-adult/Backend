from sqlalchemy.orm import Session
from sqlalchemy import and_, or_, desc, func
from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta
import hashlib

from models import (
    CrawlJob,
    CrawlTask,
    CrawlNotice,
    HostBudget,
    Webhook,
    DetailCategory,
    JobStatus,
    TaskStatus,
    SeedType,
    RenderMode,
    RobotsPolicy,
)


# ==================== CrawlJob CRUD ====================


def create_job(db: Session, job_data: Dict[str, Any]) -> CrawlJob:
    """새로운 크롤 잡 생성"""
    db_job = CrawlJob(**job_data)
    db.add(db_job)
    db.commit()
    db.refresh(db_job)
    return db_job


def get_job(db: Session, job_id: int) -> Optional[CrawlJob]:
    """ID로 잡 조회"""
    return db.query(CrawlJob).filter(CrawlJob.id == job_id).first()


def get_job_by_name(db: Session, name: str) -> Optional[CrawlJob]:
    """이름으로 잡 조회"""
    return db.query(CrawlJob).filter(CrawlJob.name == name).first()


def get_jobs(
    db: Session,
    skip: int = 0,
    limit: int = 100,
    status: Optional[JobStatus] = None,
    priority: Optional[str] = None,
) -> List[CrawlJob]:
    """잡 목록 조회 (필터링 및 페이징)"""
    query = db.query(CrawlJob)

    if status:
        query = query.filter(CrawlJob.status == status)
    if priority:
        query = query.filter(CrawlJob.priority == priority)

    return query.offset(skip).limit(limit).all()


def update_job_status(
    db: Session, job_id: int, status: JobStatus
) -> Optional[CrawlJob]:
    """잡 상태 업데이트"""
    db_job = get_job(db, job_id)
    if db_job:
        db_job.status = status
        db_job.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(db_job)
    return db_job


def delete_job(db: Session, job_id: int) -> bool:
    """잡 삭제"""
    db_job = get_job(db, job_id)
    if db_job:
        db.delete(db_job)
        db.commit()
        return True
    return False


# ==================== CrawlTask CRUD ====================


def create_task(db: Session, task_data: Dict[str, Any]) -> CrawlTask:
    """새로운 크롤 태스크 생성"""
    db_task = CrawlTask(**task_data)
    db.add(db_task)
    db.commit()
    db.refresh(db_task)
    return db_task


def get_task(db: Session, task_id: int) -> Optional[CrawlTask]:
    """ID로 태스크 조회"""
    return db.query(CrawlTask).filter(CrawlTask.id == task_id).first()


def get_tasks(
    db: Session,
    skip: int = 0,
    limit: int = 100,
    status: Optional[TaskStatus] = None,
    job_id: Optional[int] = None,
) -> List[CrawlTask]:
    """태스크 목록 조회 (필터링 및 페이징)"""
    query = db.query(CrawlTask)

    if status:
        query = query.filter(CrawlTask.status == status)
    if job_id:
        query = query.filter(CrawlTask.job_id == job_id)

    return query.order_by(desc(CrawlTask.created_at)).offset(skip).limit(limit).all()


def get_tasks_by_job(
    db: Session,
    job_id: int,
    status: Optional[TaskStatus] = None,
    skip: int = 0,
    limit: int = 100,
) -> List[CrawlTask]:
    """잡 ID로 태스크 목록 조회"""
    query = db.query(CrawlTask).filter(CrawlTask.job_id == job_id)

    if status:
        query = query.filter(CrawlTask.status == status)

    return query.offset(skip).limit(limit).all()


def update_task_status(
    db: Session,
    task_id: int,
    status: TaskStatus,
    error: Optional[str] = None,
    http_status: Optional[int] = None,
    content_hash: Optional[str] = None,
) -> Optional[CrawlTask]:
    """태스크 상태 업데이트"""
    db_task = get_task(db, task_id)
    if db_task:
        db_task.status = status
        db_task.last_error = error
        db_task.http_status = http_status
        db_task.content_hash = content_hash

        if status == TaskStatus.RUNNING:
            db_task.started_at = datetime.utcnow()
        elif status in [TaskStatus.SUCCESS, TaskStatus.FAILED, TaskStatus.BLOCKED]:
            db_task.finished_at = datetime.utcnow()

        db.commit()
        db.refresh(db_task)
    return db_task


def increment_task_retries(db: Session, task_id: int) -> Optional[CrawlTask]:
    """태스크 재시도 횟수 증가"""
    db_task = get_task(db, task_id)
    if db_task:
        db_task.retries += 1
        db.commit()
        db.refresh(db_task)
    return db_task


# ==================== CrawlNotice CRUD ====================


def create_document(db: Session, doc_data: Dict[str, Any]) -> CrawlNotice:
    """새로운 추출 문서 생성 (detail_category 자동 업데이트 포함)"""
    # category 필드가 있으면 detail_category 테이블에 자동 추가
    if doc_data.get("category"):
        ensure_detail_category(db, doc_data["category"])

    db_doc = CrawlNotice(**doc_data)
    db.add(db_doc)
    db.commit()
    db.refresh(db_doc)
    return db_doc


def bulk_create_documents(db: Session, docs_data: List[Dict[str, Any]]) -> int:
    """
    문서 벌크 생성 (대량 삽입 최적화, detail_category 자동 업데이트 포함)

    Args:
        db: 데이터베이스 세션
        docs_data: 문서 데이터 리스트

    Returns:
        int: 삽입된 문서 수
    """
    if not docs_data:
        return 0

    try:
        # 모든 문서의 category 값 추출하여 detail_category 테이블에 일괄 추가
        category_names = [
            doc.get("category") for doc in docs_data
            if doc.get("category")
        ]
        if category_names:
            ensure_detail_categories_bulk(db, category_names)

        # SQLAlchemy bulk_insert_mappings 사용 (가장 빠름)
        db.bulk_insert_mappings(CrawlNotice, docs_data)
        db.commit()
        return len(docs_data)
    except Exception as e:
        db.rollback()
        raise e


def get_document(db: Session, doc_id: int) -> Optional[CrawlNotice]:
    """ID로 문서 조회"""
    return db.query(CrawlNotice).filter(CrawlNotice.id == doc_id).first()


def get_documents(
    db: Session,
    skip: int = 0,
    limit: int = 100,
    source: Optional[str] = None,
    category: Optional[str] = None,
) -> List[CrawlNotice]:
    """문서 목록 조회 (필터링 및 페이징)"""
    query = db.query(CrawlNotice)

    if source:
        query = query.filter(CrawlNotice.source == source)
    if category:
        query = query.filter(CrawlNotice.category == category)

    return query.order_by(desc(CrawlNotice.date), desc(CrawlNotice.created_at)).offset(skip).limit(limit).all()


def get_documents_by_job(
    db: Session, job_id: int, skip: int = 0, limit: int = 100
) -> List[CrawlNotice]:
    """잡 ID로 문서 목록 조회"""
    return (
        db.query(CrawlNotice)
        .filter(CrawlNotice.job_id == job_id)
        .order_by(desc(CrawlNotice.date), desc(CrawlNotice.created_at))
        .offset(skip)
        .limit(limit)
        .all()
    )


def get_document_by_url(db: Session, url: str) -> Optional[CrawlNotice]:
    """URL로 문서 조회"""
    return db.query(CrawlNotice).filter(CrawlNotice.url == url).first()


def search_documents(
    db: Session,
    job_id: Optional[int] = None,
    url: Optional[str] = None,
    q: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
) -> List[CrawlNotice]:
    """문서 검색"""
    query = db.query(CrawlNotice)

    if job_id:
        query = query.filter(CrawlNotice.job_id == job_id)
    if url:
        query = query.filter(CrawlNotice.url.contains(url))
    if q:
        # JSON 필드에서 검색 (PostgreSQL의 경우)
        query = query.filter(
            or_(CrawlNotice.url.contains(q), CrawlNotice.raw.contains(q))
        )

    return query.order_by(desc(CrawlNotice.date), desc(CrawlNotice.created_at)).offset(skip).limit(limit).all()


# ==================== HostBudget CRUD ====================


def get_host_budget(db: Session, host: str) -> Optional[HostBudget]:
    """호스트별 예산 조회"""
    return db.query(HostBudget).filter(HostBudget.host == host).first()


def create_or_update_host_budget(
    db: Session, host: str, budget_data: Dict[str, Any]
) -> HostBudget:
    """호스트별 예산 생성 또는 업데이트"""
    db_budget = get_host_budget(db, host)
    if db_budget:
        for key, value in budget_data.items():
            setattr(db_budget, key, value)
    else:
        db_budget = HostBudget(host=host, **budget_data)
        db.add(db_budget)

    db.commit()
    db.refresh(db_budget)
    return db_budget


def increment_browser_usage(db: Session, host: str, seconds: int) -> bool:
    """호스트별 브라우저 사용 시간 증가"""
    db_budget = get_host_budget(db, host)
    if db_budget:
        db_budget.used_today_sec += seconds
        db.commit()
        return True
    return False


def reset_daily_budget(db: Session) -> int:
    """일일 예산 초기화 (매일 자정에 실행)"""
    result = db.query(HostBudget).update({HostBudget.used_today_sec: 0})
    db.commit()
    return result


# ==================== Webhook CRUD ====================


def create_webhook(db: Session, webhook_data: Dict[str, Any]) -> Webhook:
    """새로운 웹훅 생성"""
    db_webhook = Webhook(**webhook_data)
    db.add(db_webhook)
    db.commit()
    db.refresh(db_webhook)
    return db_webhook


def get_webhooks_by_job(db: Session, job_id: int) -> List[Webhook]:
    """잡 ID로 웹훅 목록 조회"""
    return db.query(Webhook).filter(Webhook.job_id == job_id).all()


def delete_webhook(db: Session, webhook_id: int) -> bool:
    """웹훅 삭제"""
    db_webhook = db.query(Webhook).filter(Webhook.id == webhook_id).first()
    if db_webhook:
        db.delete(db_webhook)
        db.commit()
        return True
    return False


# ==================== 유틸리티 함수 ====================


def get_job_statistics(db: Session, job_id: int) -> Dict[str, Any]:
    """잡 통계 정보 조회"""
    total_tasks = db.query(CrawlTask).filter(CrawlTask.job_id == job_id).count()
    completed_tasks = (
        db.query(CrawlTask)
        .filter(
            and_(CrawlTask.job_id == job_id, CrawlTask.status == TaskStatus.SUCCESS)
        )
        .count()
    )
    failed_tasks = (
        db.query(CrawlTask)
        .filter(and_(CrawlTask.job_id == job_id, CrawlTask.status == TaskStatus.FAILED))
        .count()
    )
    blocked_tasks = (
        db.query(CrawlTask)
        .filter(and_(CrawlTask.job_id == job_id, CrawlTask.blocked_flag == True))
        .count()
    )

    return {
        "total_tasks": total_tasks,
        "completed_tasks": completed_tasks,
        "failed_tasks": failed_tasks,
        "blocked_tasks": blocked_tasks,
        "success_rate": (completed_tasks / total_tasks * 100) if total_tasks > 0 else 0,
    }


def get_host_statistics(db: Session) -> List[Dict[str, Any]]:
    """호스트별 통계 정보 조회"""
    stats = (
        db.query(
            HostBudget.host,
            HostBudget.qps_limit,
            HostBudget.concurrency_limit,
            HostBudget.daily_budget_sec_browser,
            HostBudget.used_today_sec,
            func.count(CrawlTask.id).label("total_tasks"),
            func.count(CrawlTask.id)
            .filter(CrawlTask.status == TaskStatus.SUCCESS)
            .label("success_tasks"),
            func.count(CrawlTask.id)
            .filter(CrawlTask.blocked_flag == True)
            .label("blocked_tasks"),
        )
        .outerjoin(CrawlTask, HostBudget.host == func.substring(CrawlTask.url, 8, 50))
        .group_by(
            HostBudget.host,
            HostBudget.qps_limit,
            HostBudget.concurrency_limit,
            HostBudget.daily_budget_sec_browser,
            HostBudget.used_today_sec,
        )
        .all()
    )

    return [
        {
            "host": stat.host,
            "qps_limit": stat.qps_limit,
            "concurrency_limit": stat.concurrency_limit,
            "daily_budget_sec_browser": stat.daily_budget_sec_browser,
            "used_today_sec": stat.used_today_sec,
            "total_tasks": stat.total_tasks,
            "success_tasks": stat.success_tasks,
            "blocked_tasks": stat.blocked_tasks,
            "success_rate": (
                (stat.success_tasks / stat.total_tasks * 100)
                if stat.total_tasks > 0
                else 0
            ),
            "budget_usage": (
                (stat.used_today_sec / stat.daily_budget_sec_browser * 100)
                if stat.daily_budget_sec_browser > 0
                else 0
            ),
        }
        for stat in stats
    ]


# ==================== Content Crawling Functions ====================


def get_notices_without_content(
    db: Session,
    limit: int = 100,
    source: Optional[str] = None
) -> List[CrawlNotice]:
    """
    content 필드가 비어있는 공지사항 조회

    Args:
        db: 데이터베이스 세션
        limit: 최대 조회 개수
        source: 카테고리 필터 (volunteer, job, scholarship 등)

    Returns:
        content가 NULL이거나 빈 문자열인 CrawlNotice 리스트
    """
    query = db.query(CrawlNotice).filter(
        or_(
            CrawlNotice.content == None,
            CrawlNotice.content == ""
        )
    )

    if source:
        query = query.filter(CrawlNotice.source == source)

    return query.order_by(desc(CrawlNotice.created_at)).limit(limit).all()


def update_notice_content(
    db: Session,
    notice_id: int,
    content: str
) -> Optional[CrawlNotice]:
    """
    공지사항의 content 필드 업데이트

    Args:
        db: 데이터베이스 세션
        notice_id: 공지사항 ID
        content: 업데이트할 본문 내용

    Returns:
        업데이트된 CrawlNotice 객체 (실패 시 None)
    """
    db_notice = get_document(db, notice_id)
    if db_notice:
        db_notice.content = content
        db_notice.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(db_notice)
    return db_notice


def bulk_update_notice_contents(
    db: Session,
    updates: List[Dict[str, Any]]
) -> int:
    """
    여러 공지사항의 content 필드를 일괄 업데이트

    Args:
        db: 데이터베이스 세션
        updates: 업데이트 데이터 리스트
                 [{id: int, content: str}, ...]

    Returns:
        업데이트된 레코드 수
    """
    if not updates:
        return 0

    try:
        # 현재 시각
        now = datetime.utcnow()

        # 벌크 업데이트 준비
        update_mappings = []
        for update in updates:
            update_mappings.append({
                "id": update["id"],
                "content": update["content"],
                "updated_at": now
            })

        # SQLAlchemy bulk_update_mappings 사용
        db.bulk_update_mappings(CrawlNotice, update_mappings)
        db.commit()
        return len(update_mappings)

    except Exception as e:
        db.rollback()
        raise e


# ==================== DetailCategory CRUD ====================


def ensure_detail_category(db: Session, category_name: str) -> Optional[DetailCategory]:
    """
    상세 카테고리가 존재하지 않으면 자동으로 생성

    Args:
        db: 데이터베이스 세션
        category_name: 카테고리 이름

    Returns:
        DetailCategory 객체 (이미 존재하거나 새로 생성된)
    """
    if not category_name or category_name.strip() == "":
        return None

    category_name = category_name.strip()

    # 기존 카테고리 조회
    existing = db.query(DetailCategory).filter(
        DetailCategory.name == category_name
    ).first()

    if existing:
        return existing

    # 새 카테고리 생성
    try:
        new_category = DetailCategory(
            name=category_name,
            description=f"자동 생성된 카테고리: {category_name}",
            is_active=True
        )
        db.add(new_category)
        db.commit()
        db.refresh(new_category)
        return new_category
    except Exception:
        # 동시성 문제로 이미 생성된 경우 다시 조회
        db.rollback()
        return db.query(DetailCategory).filter(
            DetailCategory.name == category_name
        ).first()


def ensure_detail_categories_bulk(db: Session, category_names: List[str]) -> int:
    """
    여러 상세 카테고리를 일괄적으로 확인하고 없는 것만 생성

    Args:
        db: 데이터베이스 세션
        category_names: 카테고리 이름 리스트

    Returns:
        새로 생성된 카테고리 수
    """
    if not category_names:
        return 0

    # 중복 제거 및 빈 값 필터링
    unique_names = set(
        name.strip() for name in category_names
        if name and name.strip()
    )

    if not unique_names:
        return 0

    # 기존 카테고리 조회
    existing = db.query(DetailCategory.name).filter(
        DetailCategory.name.in_(unique_names)
    ).all()
    existing_names = {row[0] for row in existing}

    # 새로 추가할 카테고리
    new_names = unique_names - existing_names

    if not new_names:
        return 0

    try:
        # 새 카테고리 일괄 생성
        new_categories = [
            {
                "name": name,
                "description": f"자동 생성된 카테고리: {name}",
                "is_active": True,
                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow()
            }
            for name in new_names
        ]
        db.bulk_insert_mappings(DetailCategory, new_categories)
        db.commit()
        return len(new_categories)
    except Exception:
        db.rollback()
        return 0


def get_all_detail_categories(db: Session) -> List[DetailCategory]:
    """모든 상세 카테고리 조회"""
    return db.query(DetailCategory).order_by(DetailCategory.name).all()


def get_detail_category_by_name(db: Session, name: str) -> Optional[DetailCategory]:
    """이름으로 상세 카테고리 조회"""
    return db.query(DetailCategory).filter(DetailCategory.name == name).first()
