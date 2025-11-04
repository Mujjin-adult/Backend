from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List, Optional
from pydantic import BaseModel

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

router = APIRouter()


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
@router.post("/jobs")
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
@router.get("/jobs/{job_id}")
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
@router.post("/jobs/{job_id}/run")
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
@router.post("/jobs/{job_id}/{action}")
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


# 추출 결과 검색
@router.get("/docs")
def search_docs(
    db: Session = Depends(get_db),
    job_id: Optional[int] = Query(None, description="Filter by job ID"),
    url: Optional[str] = Query(None, description="Filter by URL (partial match)"),
    q: Optional[str] = Query(None, description="Search query for title or content"),
    limit: int = Query(50, ge=1, le=200)
):
    """추출된 문서 검색"""
    try:
        # 기본 쿼리
        query = text("""
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
            WHERE 1=1
                {job_filter}
                {url_filter}
                {search_filter}
            ORDER BY created_at DESC
            LIMIT :limit
        """)

        # 필터 조건 추가
        filters = []
        params = {"limit": limit}

        if job_id is not None:
            filters.append("AND job_id = :job_id")
            params["job_id"] = job_id

        if url:
            filters.append("AND url ILIKE :url")
            params["url"] = f"%{url}%"

        if q:
            filters.append("AND (title ILIKE :q OR raw ILIKE :q)")
            params["q"] = f"%{q}%"

        # 쿼리 완성
        query_str = str(query).format(
            job_filter=filters[0] if len(filters) > 0 and job_id else "",
            url_filter=filters[1] if len(filters) > 1 and url else (filters[0] if url and not job_id else ""),
            search_filter=filters[-1] if q else ""
        )

        # 간단하게 다시 작성
        base_conditions = ["1=1"]
        if job_id is not None:
            base_conditions.append("job_id = :job_id")
        if url:
            base_conditions.append("url ILIKE :url")
        if q:
            base_conditions.append("(title ILIKE :q OR raw ILIKE :q)")

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
            ORDER BY created_at DESC
            LIMIT :limit
        """)

        results = db.execute(final_query, params).fetchall()

        return {
            "filters": {
                "job_id": job_id,
                "url": url,
                "query": q
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
@router.get("/health")
def health():
    return {"status": "ok"}


# 메트릭 엔드포인트 (Prometheus)
@router.get("/metrics")
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


@router.get("/documents")
def get_all_documents(
    db: Session = Depends(get_db),
    limit: int = Query(100, ge=1, le=1000),
    offset: int = Query(0, ge=0),
    source: Optional[str] = Query(
        None, description="Filter by source (e.g., volunteer, job, scholarship)"
    ),
    category: Optional[str] = Query(None, description="Filter by category"),
):
    """모든 추출된 문서 조회"""
    documents = get_documents(
        db, limit=limit, offset=offset, source=source, category=category
    )
    return {"documents": documents, "total": len(documents)}


@router.get("/documents/summary")
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


@router.get("/documents/recent")
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
            ORDER BY created_at DESC
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


@router.get("/documents/search")
def search_documents(
    db: Session = Depends(get_db),
    q: str = Query(..., description="Search query"),
    source: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=200),
):
    """문서 검색"""
    try:
        # 제목이나 내용에서 검색
        search_query = f"%{q}%"

        if source:
            results = db.execute(
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
                WHERE (title ILIKE :query OR raw ILIKE :query)
                AND source = :source
                ORDER BY created_at DESC
                LIMIT :limit
            """),
                {"query": search_query, "source": source, "limit": limit},
            ).fetchall()
        else:
            results = db.execute(
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
                WHERE title ILIKE :query OR raw ILIKE :query
                ORDER BY created_at DESC
                LIMIT :limit
            """),
                {"query": search_query, "limit": limit},
            ).fetchall()

        return {
            "query": q,
            "source": source,
            "results": [
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
                for doc in results
            ],
            "total_found": len(results),
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")


@router.get("/crawling-status")
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
