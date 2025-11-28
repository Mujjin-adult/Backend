from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List, Optional
from pydantic import BaseModel
import logging

from database import get_db
from crud import (
    create_job,
    get_job,
    get_jobs,
    update_job_status,
    create_task,
    get_task,
    get_tasks,
    update_task_status,
    create_document,
    get_documents,
    get_documents_by_job,
    get_job_statistics,
    get_host_statistics,
)
import schemas

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/api/v1",
    responses={404: {"description": "Not found"}},
)


# 잡 생성 요청 스키마
class JobCreateRequest(BaseModel):
    name: str
    priority: str
    seed_type: str
    seed_payload: dict
    render_mode: str
    rate_limit_per_host: Optional[float] = 1.0
    max_depth: Optional[int] = 1
    robots_policy: str
    schedule_cron: Optional[str] = None


# 잡 생성
@router.post(
    "/jobs",
    tags=["크롤링 작업 관리"],
    summary="새로운 크롤링 작업 생성",
    description="새로운 크롤링 작업을 생성하고 스케줄에 등록합니다."
)
def create_job_endpoint(req: JobCreateRequest, db: Session = Depends(get_db)):
    """크롤링 잡 생성"""
    try:
        # DB에 crawl_job 저장
        job_data = {
            "name": req.name,
            "priority": req.priority,
            "seed_type": req.seed_type,
            "seed_payload": req.seed_payload,
            "render_mode": req.render_mode,
            "rate_limit_per_host": req.rate_limit_per_host,
            "max_depth": req.max_depth,
            "robots_policy": req.robots_policy,
            "schedule_cron": req.schedule_cron,
        }

        new_job = create_job(db, job_data)

        # Celery Beat에 스케줄 등록 (cron이 있는 경우)
        if req.schedule_cron:
            from auto_scheduler import get_auto_scheduler
            scheduler = get_auto_scheduler()
            scheduler.update_celery_schedule()

        return {
            "status": "created",
            "job_id": new_job.id,
            "job": {
                "id": new_job.id,
                "name": new_job.name,
                "priority": new_job.priority,
                "status": new_job.status,
                "created_at": new_job.created_at,
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create job: {str(e)}")


# 잡 조회
@router.get(
    "/jobs/{job_id}",
    tags=["크롤링 작업 관리"],
    summary="특정 크롤링 작업 조회",
    description="ID로 특정 크롤링 작업의 상세 정보를 조회합니다."
)
def get_job_endpoint(job_id: int, db: Session = Depends(get_db)):
    """특정 크롤링 잡 조회"""
    job = get_job(db, job_id)
    if not job:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    # 연관된 태스크 및 문서 수 조회
    task_count = db.execute(
        text("SELECT COUNT(*) FROM crawl_task WHERE job_id = :job_id"),
        {"job_id": job_id}
    ).scalar()

    doc_count = db.execute(
        text("SELECT COUNT(*) FROM crawl_notice WHERE job_id = :job_id"),
        {"job_id": job_id}
    ).scalar()

    return {
        "job_id": job.id,
        "name": job.name,
        "priority": job.priority,
        "status": job.status,
        "schedule_cron": job.schedule_cron,
        "created_at": job.created_at,
        "updated_at": job.updated_at,
        "task_count": task_count,
        "document_count": doc_count,
    }


# 수동 트리거 (더 구체적인 경로를 먼저 정의)
@router.post(
    "/jobs/{job_id}/run",
    tags=["크롤링 작업 관리"],
    summary="크롤링 작업 수동 실행",
    description="특정 크롤링 작업을 즉시 실행합니다 (백그라운드)."
)
def run_job(job_id: int, db: Session = Depends(get_db)):
    """특정 크롤링 잡 수동 실행"""
    # 잡 조회
    job = get_job(db, job_id)
    if not job:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    try:
        # Celery 태스크 트리거
        from tasks import college_crawl_task
        task = college_crawl_task.delay(job.name)

        return {
            "job_id": job_id,
            "job_name": job.name,
            "triggered": True,
            "task_id": task.id,
            "message": f"Crawling task for '{job.name}' has been triggered"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to trigger job: {str(e)}"
        )


# 잡 상태 변경
@router.post(
    "/jobs/{job_id}/{action}",
    tags=["크롤링 작업 관리"],
    summary="크롤링 작업 상태 변경",
    description="크롤링 작업을 일시정지(pause), 재개(resume), 취소(cancel)합니다."
)
def job_action(job_id: int, action: str, db: Session = Depends(get_db)):
    """크롤링 잡 상태 변경 (pause/resume/cancel)"""
    # 유효한 액션 확인
    valid_actions = ["pause", "resume", "cancel"]
    if action not in valid_actions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid action: {action}. Must be one of {valid_actions}"
        )

    # 잡 조회
    job = get_job(db, job_id)
    if not job:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    try:
        # DB 상태 변경
        if action == "pause":
            new_status = "PAUSED"
        elif action == "resume":
            new_status = "ACTIVE"
        elif action == "cancel":
            new_status = "CANCELLED"

        update_job_status(db, job_id, new_status)

        # Celery Beat 스케줄 업데이트
        from auto_scheduler import get_auto_scheduler
        scheduler = get_auto_scheduler()
        scheduler.update_celery_schedule()

        return {
            "job_id": job_id,
            "action": action,
            "previous_status": job.status,
            "new_status": new_status,
            "result": "success"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to {action} job: {str(e)}"
        )


# 작업 ID 및 카테고리로 문서 검색
@router.get(
    "/docs",
    tags=["문서 관리"],
    summary="문서 검색 (고급)",
    description="작업 ID 또는 카테고리로 크롤링된 문서를 검색합니다."
)
def search_docs(
    db: Session = Depends(get_db),
    job_id: Optional[int] = Query(None, description="작업 ID로 필터링"),
    category: Optional[str] = Query(None, description="카테고리로 필터링 (예: 봉사, 장학금 등)"),
    limit: int = Query(50, ge=1, le=200, description="최대 결과 수")
):
    """작업 ID 또는 카테고리로 문서 검색"""
    try:
        base_conditions = ["1=1"]
        params = {"limit": limit}

        if job_id is not None:
            base_conditions.append("job_id = :job_id")
            params["job_id"] = job_id

        if category:
            base_conditions.append("category ILIKE :category")
            params["category"] = f"%{category}%"

        final_query = text(f"""
            SELECT
                id,
                job_id,
                url,
                title as title,
                writer as writer,
                date as date,
                source as source,
                category as category,
                created_at
            FROM crawl_notice
            WHERE {' AND '.join(base_conditions)}
            ORDER BY date DESC NULLS LAST, created_at DESC
            LIMIT :limit
        """)

        results = db.execute(final_query, params).fetchall()

        return {
            "filters": {
                "job_id": job_id,
                "category": category
            },
            "results": [
                {
                    "id": doc.id,
                    "job_id": doc.job_id,
                    "url": doc.url,
                    "title": doc.title,
                    "writer": doc.writer,
                    "date": doc.date,
                    "source": doc.source,
                    "category": doc.category,
                    "created_at": doc.created_at,
                }
                for doc in results
            ],
            "total_found": len(results)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")


# 헬스 체크
@router.get(
    "/health",
    tags=["시스템 상태"],
    summary="간단한 헬스 체크",
    description="API 서버의 기본 상태를 확인합니다."
)
def health():
    return {"status": "ok"}


# 메트릭 엔드포인트 (Prometheus)
@router.get(
    "/metrics",
    tags=["모니터링"],
    summary="Prometheus 메트릭",
    description="시스템 메트릭을 Prometheus 형식으로 반환합니다."
)
def metrics(db: Session = Depends(get_db)):
    """Prometheus 메트릭 반환"""
    try:
        from fastapi.responses import PlainTextResponse

        # 총 잡 수
        total_jobs = db.execute(text("SELECT COUNT(*) FROM crawl_job")).scalar()

        # 상태별 잡 수
        active_jobs = db.execute(
            text("SELECT COUNT(*) FROM crawl_job WHERE status = 'ACTIVE'")
        ).scalar()
        paused_jobs = db.execute(
            text("SELECT COUNT(*) FROM crawl_job WHERE status = 'PAUSED'")
        ).scalar()

        # 총 태스크 수 및 상태별 태스크 수
        total_tasks = db.execute(text("SELECT COUNT(*) FROM crawl_task")).scalar()
        success_tasks = db.execute(
            text("SELECT COUNT(*) FROM crawl_task WHERE status = 'SUCCESS'")
        ).scalar()
        failed_tasks = db.execute(
            text("SELECT COUNT(*) FROM crawl_task WHERE status = 'FAILED'")
        ).scalar()
        pending_tasks = db.execute(
            text("SELECT COUNT(*) FROM crawl_task WHERE status = 'PENDING'")
        ).scalar()

        # 총 문서 수
        total_docs = db.execute(text("SELECT COUNT(*) FROM crawl_notice")).scalar()

        # 소스별 문서 수
        source_counts = db.execute(
            text("""
                SELECT source as source, COUNT(*) as count
                FROM crawl_notice
                WHERE source IS NOT NULL
                GROUP BY source
            """)
        ).fetchall()

        # Prometheus 포맷으로 메트릭 생성
        metrics_output = []

        # 잡 메트릭
        metrics_output.append("# HELP crawler_jobs_total Total number of crawler jobs")
        metrics_output.append("# TYPE crawler_jobs_total gauge")
        metrics_output.append(f"crawler_jobs_total {total_jobs}")
        metrics_output.append("")

        metrics_output.append("# HELP crawler_jobs_by_status Number of jobs by status")
        metrics_output.append("# TYPE crawler_jobs_by_status gauge")
        metrics_output.append(f'crawler_jobs_by_status{{status="active"}} {active_jobs}')
        metrics_output.append(f'crawler_jobs_by_status{{status="paused"}} {paused_jobs}')
        metrics_output.append("")

        # 태스크 메트릭
        metrics_output.append("# HELP crawler_tasks_total Total number of crawler tasks")
        metrics_output.append("# TYPE crawler_tasks_total counter")
        metrics_output.append(f"crawler_tasks_total {total_tasks}")
        metrics_output.append("")

        metrics_output.append("# HELP crawler_tasks_by_status Number of tasks by status")
        metrics_output.append("# TYPE crawler_tasks_by_status gauge")
        metrics_output.append(f'crawler_tasks_by_status{{status="success"}} {success_tasks}')
        metrics_output.append(f'crawler_tasks_by_status{{status="failed"}} {failed_tasks}')
        metrics_output.append(f'crawler_tasks_by_status{{status="pending"}} {pending_tasks}')
        metrics_output.append("")

        # 문서 메트릭
        metrics_output.append("# HELP crawler_documents_total Total number of extracted documents")
        metrics_output.append("# TYPE crawler_documents_total counter")
        metrics_output.append(f"crawler_documents_total {total_docs}")
        metrics_output.append("")

        metrics_output.append("# HELP crawler_documents_by_source Number of documents by source")
        metrics_output.append("# TYPE crawler_documents_by_source gauge")
        for row in source_counts:
            source = row.source or "unknown"
            count = row.count
            metrics_output.append(f'crawler_documents_by_source{{source="{source}"}} {count}')

        return PlainTextResponse(
            content="\n".join(metrics_output),
            media_type="text/plain; version=0.0.4"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate metrics: {str(e)}")


@router.get(
    "/documents",
    tags=["문서 관리"],
    summary="전체 문서 목록 조회",
    description="카테고리별로 크롤링된 문서를 페이징하여 조회합니다. 카테고리를 지정하지 않으면 모든 문서를 반환합니다."
)
def get_all_documents(
    db: Session = Depends(get_db),
    category: Optional[str] = Query(None, description="카테고리로 필터링 (예: 봉사, 장학금, 학점 등). 비어있으면 전체 조회"),
    limit: int = Query(100, ge=1, le=1000, description="페이지 당 문서 수"),
    offset: int = Query(0, ge=0, description="건너뛸 문서 수"),
):
    """카테고리별 또는 전체 문서 조회"""
    try:
        # 카테고리 필터링
        if category:
            # 특정 카테고리만 조회
            documents = get_documents(
                db, skip=offset, limit=limit, category=category
            )
            # 전체 개수 조회
            total_count = db.execute(
                text("SELECT COUNT(*) FROM crawl_notice WHERE category ILIKE :category"),
                {"category": f"%{category}%"}
            ).scalar()
        else:
            # 모든 카테고리 조회
            documents = get_documents(
                db, skip=offset, limit=limit
            )
            # 전체 개수 조회
            total_count = db.execute(
                text("SELECT COUNT(*) FROM crawl_notice")
            ).scalar()

        return {
            "filter": {
                "category": category
            },
            "documents": [
                {
                    "id": doc.id,
                    "job_id": doc.job_id,
                    "title": doc.title,
                    "writer": doc.writer,
                    "date": doc.date,
                    "hits": doc.hits,
                    "url": doc.url,
                    "source": doc.source,
                    "category": doc.category,
                    "created_at": doc.created_at,
                }
                for doc in documents
            ],
            "pagination": {
                "offset": offset,
                "limit": limit,
                "returned": len(documents),
                "total": total_count
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get documents: {str(e)}")


@router.get(
    "/documents/summary",
    tags=["문서 관리"],
    summary="문서 요약 통계",
    description="소스별, 카테고리별 문서 통계 정보를 조회합니다."
)
def get_documents_summary(db: Session = Depends(get_db)):
    """문서 요약 통계 조회"""
    try:
        # 소스별 문서 수 (JSON 필드에서 source 추출)
        source_stats = db.execute(
            text("""
            SELECT source as source, COUNT(*) as count, MAX(created_at) as latest_update
            FROM crawl_notice
            WHERE source IS NOT NULL
            GROUP BY source
            ORDER BY count DESC
        """)
        ).fetchall()

        # 카테고리별 문서 수 (JSON 필드에서 category 추출)
        category_stats = db.execute(
            text("""
            SELECT category as category, COUNT(*) as count
            FROM crawl_notice
            WHERE category IS NOT NULL
            GROUP BY category
            ORDER BY count DESC
        """)
        ).fetchall()

        # 전체 통계
        total_docs = db.execute(text("SELECT COUNT(*) FROM crawl_notice")).scalar()
        latest_doc = db.execute(text("SELECT MAX(created_at) FROM crawl_notice")).scalar()

        return {
            "total_documents": total_docs,
            "latest_update": latest_doc,
            "source_statistics": [
                {
                    "source": row.source,
                    "count": row.count,
                    "latest_update": row.latest_update,
                }
                for row in source_stats
            ],
            "category_statistics": [
                {"category": row.category, "count": row.count} for row in category_stats
            ],
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get summary: {str(e)}")


@router.get(
    "/documents/recent",
    tags=["문서 관리"],
    summary="최근 크롤링된 문서 조회",
    description="최근에 크롤링된 문서 목록을 최신순으로 조회합니다."
)
def get_recent_documents(
    db: Session = Depends(get_db), limit: int = Query(20, ge=1, le=100)
):
    """최근 크롤링된 문서 조회"""
    try:
        recent_docs = db.execute(
            text("""
            SELECT
                title as title,
                writer as writer,
                date as date,
                hits as hits,
                url,
                source as source,
                category as category,
                created_at
            FROM crawl_notice
            ORDER BY date DESC NULLS LAST, created_at DESC
            LIMIT :limit
        """),
            {"limit": limit},
        ).fetchall()

        return {
            "documents": [
                {
                    "title": doc.title,
                    "writer": doc.writer,
                    "date": doc.date,
                    "hits": doc.hits,
                    "url": doc.url,
                    "source": doc.source,
                    "category": doc.category,
                    "created_at": doc.created_at,
                }
                for doc in recent_docs
            ]
        }
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to get recent documents: {str(e)}"
        )


@router.get(
    "/documents/search/title",
    tags=["문서 관리"],
    summary="문서 제목으로 검색",
    description="문서의 제목에서 키워드를 검색합니다."
)
def search_by_title(
    db: Session = Depends(get_db),
    q: str = Query(..., description="검색 키워드"),
    limit: int = Query(50, ge=1, le=200, description="최대 결과 수"),
):
    """문서 제목 검색"""
    try:
        search_query = f"%{q}%"

        results = db.execute(
            text("""
            SELECT
                id,
                job_id,
                title as title,
                writer as writer,
                date as date,
                hits as hits,
                url,
                source as source,
                category as category,
                created_at
            FROM crawl_notice
            WHERE title ILIKE :query
            ORDER BY date DESC NULLS LAST, created_at DESC
            LIMIT :limit
        """),
            {"query": search_query, "limit": limit},
        ).fetchall()

        return {
            "search_type": "title",
            "query": q,
            "results": [
                {
                    "id": doc.id,
                    "job_id": doc.job_id,
                    "title": doc.title,
                    "writer": doc.writer,
                    "date": doc.date,
                    "hits": doc.hits,
                    "url": doc.url,
                    "source": doc.source,
                    "category": doc.category,
                    "created_at": doc.created_at,
                }
                for doc in results
            ],
            "total_found": len(results),
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Title search failed: {str(e)}")


@router.get(
    "/documents/search/content",
    tags=["문서 관리"],
    summary="문서 내용으로 검색",
    description="문서의 내용(본문)에서 키워드를 검색합니다."
)
def search_by_content(
    db: Session = Depends(get_db),
    q: str = Query(..., description="검색 키워드"),
    limit: int = Query(50, ge=1, le=200, description="최대 결과 수"),
):
    """문서 내용 검색"""
    try:
        search_query = f"%{q}%"

        results = db.execute(
            text("""
            SELECT
                id,
                job_id,
                title as title,
                writer as writer,
                date as date,
                hits as hits,
                url,
                source as source,
                category as category,
                created_at
            FROM crawl_notice
            WHERE raw ILIKE :query
            ORDER BY date DESC NULLS LAST, created_at DESC
            LIMIT :limit
        """),
            {"query": search_query, "limit": limit},
        ).fetchall()

        return {
            "search_type": "content",
            "query": q,
            "results": [
                {
                    "id": doc.id,
                    "job_id": doc.job_id,
                    "title": doc.title,
                    "writer": doc.writer,
                    "date": doc.date,
                    "hits": doc.hits,
                    "url": doc.url,
                    "source": doc.source,
                    "category": doc.category,
                    "created_at": doc.created_at,
                }
                for doc in results
            ],
            "total_found": len(results),
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Content search failed: {str(e)}")


@router.get(
    "/crawling-status",
    tags=["시스템 상태"],
    summary="크롤링 작업 상태 조회",
    description="모든 크롤링 작업의 현재 상태와 최근 활동 내역을 조회합니다."
)
def get_crawling_status(db: Session = Depends(get_db)):
    """크롤링 상태 및 통계 조회"""
    try:
        # 작업별 상태
        job_status = db.execute(
            text("""
            SELECT
                name, status, created_at, updated_at,
                (SELECT COUNT(*) FROM crawl_task WHERE job_id = cj.id) as task_count,
                (SELECT COUNT(*) FROM crawl_notice WHERE job_id = cj.id) as doc_count
            FROM crawl_job cj
            ORDER BY created_at DESC
        """)
        ).fetchall()

        # 최근 크롤링 활동
        recent_activity = db.execute(
            text("""
            SELECT
                ct.job_id,
                cj.name as job_name,
                ct.status,
                ct.started_at,
                ct.finished_at
            FROM crawl_task ct
            JOIN crawl_job cj ON ct.job_id = cj.id
            ORDER BY ct.started_at DESC
            LIMIT 10
        """)
        ).fetchall()

        return {
            "job_status": [
                {
                    "name": job.name,
                    "status": job.status,
                    "created_at": job.created_at,
                    "updated_at": job.updated_at,
                    "task_count": job.task_count,
                    "doc_count": job.doc_count,
                }
                for job in job_status
            ],
            "recent_activity": [
                {
                    "job_id": activity.job_id,
                    "job_name": activity.job_name,
                    "status": activity.status,
                    "started_at": activity.started_at,
                    "finished_at": activity.finished_at,
                }
                for activity in recent_activity
            ],
        }
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to get crawling status: {str(e)}"
        )


# ==================== Content Crawling API ====================


@router.post(
    "/crawl-content/batch",
    response_model=schemas.ContentCrawlResult,
    summary="공지사항 본문 일괄 크롤링",
    description="""
    content 필드가 비어있는 공지사항의 본문을 일괄 크롤링합니다.

    **작동 방식:**
    1. content가 NULL이거나 빈 문자열인 공지사항 조회
    2. 각 공지사항의 URL에 접속하여 본문 크롤링
    3. BeautifulSoup으로 정적 HTML 크롤링 시도
    4. 실패 시 최대 3회 재시도
    5. DB에 content 필드 업데이트

    **성능 고려:**
    - 레이트 리미팅 적용 (호스트당 1초 딜레이)
    - 배치 크기 제한 (최대 500개)
    - 비동기 처리 권장 (대량 크롤링 시)

    **사용 예시:**
    ```bash
    curl -X POST "http://localhost:8001/api/v1/crawl-content/batch" \\
      -H "Content-Type: application/json" \\
      -d '{"limit": 100, "source": "volunteer"}'
    ```
    """,
    tags=["Content Crawling"]
)
async def crawl_content_batch(
    request: schemas.ContentCrawlRequest = None,
    db: Session = Depends(get_db)
):
    """
    content 필드가 비어있는 공지사항의 본문을 일괄 크롤링

    Args:
        request: 크롤링 요청 파라미터 (limit, source)
        db: 데이터베이스 세션

    Returns:
        ContentCrawlResult: 크롤링 결과 (성공/실패 통계, 에러 목록)
    """
    import time
    from app.content_crawler import get_content_crawler
    from app.crud import get_notices_without_content, update_notice_content

    start_time = time.time()

    # 요청 파라미터 기본값 설정
    if request is None:
        request = schemas.ContentCrawlRequest(limit=100)

    limit = request.limit
    source = request.source.value if request.source else None

    logger.info(f"Starting batch content crawling: limit={limit}, source={source}")

    try:
        # content가 비어있는 공지사항 조회
        notices = get_notices_without_content(db, limit=limit, source=source)
        total_attempted = len(notices)

        if total_attempted == 0:
            logger.info("No notices found without content")
            return schemas.ContentCrawlResult(
                total_attempted=0,
                success_count=0,
                failed_count=0,
                failed_notices=[],
                execution_time_seconds=time.time() - start_time
            )

        logger.info(f"Found {total_attempted} notices without content")

        # 크롤러 인스턴스 가져오기
        crawler = get_content_crawler()

        # 크롤링 결과 저장
        success_count = 0
        failed_count = 0
        failed_notices = []

        # 각 공지사항 크롤링
        for i, notice in enumerate(notices, 1):
            logger.info(f"Crawling {i}/{total_attempted}: {notice.url}")

            # 본문 크롤링
            result = crawler.crawl_content(
                url=notice.url,
                category=notice.source
            )

            if result["success"]:
                # DB 업데이트
                updated_notice = update_notice_content(
                    db=db,
                    notice_id=notice.id,
                    content=result["content"]
                )

                if updated_notice:
                    success_count += 1
                    logger.info(f"Successfully crawled and updated notice {notice.id}")
                else:
                    failed_count += 1
                    failed_notices.append(
                        schemas.ContentCrawlErrorDetail(
                            notice_id=notice.id,
                            url=notice.url,
                            error="Failed to update database"
                        )
                    )
            else:
                # 크롤링 실패
                failed_count += 1
                failed_notices.append(
                    schemas.ContentCrawlErrorDetail(
                        notice_id=notice.id,
                        url=notice.url,
                        error=result.get("error", "Unknown error")
                    )
                )
                logger.warning(f"Failed to crawl notice {notice.id}: {result.get('error')}")

            # 레이트 리미팅 (호스트당 1-2초 딜레이)
            if i < total_attempted:
                time.sleep(1.5)

        execution_time = time.time() - start_time

        logger.info(
            f"Batch content crawling completed: "
            f"total={total_attempted}, success={success_count}, "
            f"failed={failed_count}, time={execution_time:.2f}s"
        )

        return schemas.ContentCrawlResult(
            total_attempted=total_attempted,
            success_count=success_count,
            failed_count=failed_count,
            failed_notices=failed_notices,
            execution_time_seconds=round(execution_time, 2)
        )

    except Exception as e:
        logger.error(f"Error during batch content crawling: {type(e).__name__}: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Batch content crawling failed: {str(e)}"
        )
