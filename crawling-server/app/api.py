from fastapi import APIRouter

router = APIRouter()


# 헬스 체크
@router.get("/health")
def health():
    """크롤링 서버 헬스 체크"""
    return {"status": "ok", "service": "college-crawler"}


# 메트릭 엔드포인트 (Prometheus) - 향후 확장 가능
@router.get("/metrics")
def metrics():
    """기본 메트릭 엔드포인트"""
    return {
        "status": "ok",
        "note": "Metrics collection not implemented yet. Use Celery flower for task monitoring."
    }
