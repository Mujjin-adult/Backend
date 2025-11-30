"""
Prometheus metrics for monitoring

주요 메트릭:
- HTTP 요청 수 및 응답 시간
- 크롤러 실행 횟수 및 성공/실패율
- Circuit Breaker 상태
- 데이터베이스 쿼리 수
"""

from prometheus_client import Counter, Histogram, Gauge, Info, generate_latest, CONTENT_TYPE_LATEST, REGISTRY
from fastapi import Response
import time

# ===================================================================
# HTTP 메트릭
# ===================================================================

http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'endpoint', 'status']
)

http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration in seconds',
    ['method', 'endpoint']
)

# ===================================================================
# 크롤러 메트릭
# ===================================================================

crawler_runs_total = Counter(
    'crawler_runs_total',
    'Total crawler runs',
    ['category', 'status']
)

crawler_items_scraped = Counter(
    'crawler_items_scraped',
    'Total items scraped',
    ['category']
)

crawler_duration_seconds = Histogram(
    'crawler_duration_seconds',
    'Crawler execution duration in seconds',
    ['category']
)

crawler_errors_total = Counter(
    'crawler_errors_total',
    'Total crawler errors',
    ['category', 'error_type']
)

# ===================================================================
# Circuit Breaker 메트릭
# ===================================================================

circuit_breaker_state = Gauge(
    'circuit_breaker_state',
    'Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)',
    ['name']
)

circuit_breaker_failures = Counter(
    'circuit_breaker_failures_total',
    'Total circuit breaker failures',
    ['name']
)

circuit_breaker_successes = Counter(
    'circuit_breaker_successes_total',
    'Total circuit breaker successes',
    ['name']
)

# ===================================================================
# 데이터베이스 메트릭
# ===================================================================

db_queries_total = Counter(
    'db_queries_total',
    'Total database queries',
    ['operation']
)

db_query_duration_seconds = Histogram(
    'db_query_duration_seconds',
    'Database query duration in seconds',
    ['operation']
)

db_connections_active = Gauge(
    'db_connections_active',
    'Active database connections'
)

# ===================================================================
# 시스템 메트릭
# ===================================================================

app_info = Info(
    'app',
    'Application information'
)

app_info.info({
    'version': '1.0.0',
    'name': 'College Notice Crawler'
})

celery_tasks_active = Gauge(
    'celery_tasks_active',
    'Active Celery tasks'
)

celery_workers_count = Gauge(
    'celery_workers_count',
    'Number of Celery workers'
)

# ===================================================================
# 메트릭 엔드포인트
# ===================================================================

def metrics_endpoint():
    """Prometheus metrics 엔드포인트"""
    return Response(
        content=generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )


# ===================================================================
# 유틸리티 함수
# ===================================================================

def record_http_request(method: str, endpoint: str, status: int, duration: float):
    """HTTP 요청 메트릭 기록"""
    http_requests_total.labels(method=method, endpoint=endpoint, status=status).inc()
    http_request_duration_seconds.labels(method=method, endpoint=endpoint).observe(duration)


def record_crawler_run(category: str, status: str, duration: float = None, items_count: int = 0):
    """크롤러 실행 메트릭 기록"""
    crawler_runs_total.labels(category=category, status=status).inc()

    if items_count > 0:
        crawler_items_scraped.labels(category=category).inc(items_count)

    if duration is not None:
        crawler_duration_seconds.labels(category=category).observe(duration)


def record_crawler_error(category: str, error_type: str):
    """크롤러 에러 메트릭 기록"""
    crawler_errors_total.labels(category=category, error_type=error_type).inc()


def update_circuit_breaker_state(name: str, state: str):
    """Circuit Breaker 상태 업데이트 (CLOSED=0, OPEN=1, HALF_OPEN=2)"""
    state_map = {'CLOSED': 0, 'OPEN': 1, 'HALF_OPEN': 2}
    circuit_breaker_state.labels(name=name).set(state_map.get(state, 0))


def record_circuit_breaker_failure(name: str):
    """Circuit Breaker 실패 기록"""
    circuit_breaker_failures.labels(name=name).inc()


def record_circuit_breaker_success(name: str):
    """Circuit Breaker 성공 기록"""
    circuit_breaker_successes.labels(name=name).inc()


def record_db_query(operation: str, duration: float):
    """데이터베이스 쿼리 메트릭 기록"""
    db_queries_total.labels(operation=operation).inc()
    db_query_duration_seconds.labels(operation=operation).observe(duration)


def update_celery_metrics(active_tasks: int, worker_count: int):
    """Celery 메트릭 업데이트"""
    celery_tasks_active.set(active_tasks)
    celery_workers_count.set(worker_count)
