import os
from fastapi import FastAPI, Request, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
import time
import uuid
from datetime import datetime
from contextlib import asynccontextmanager
from fastapi.responses import HTMLResponse

from api import router as api_router
from logging_config import init_logging, log_request
from database import init_database
from config import get_settings, validate_settings
from auto_scheduler import init_college_scheduler
from sentry_config import init_sentry

# 로깅 시스템 초기화
init_logging()

# Sentry 초기화
init_sentry()

# 설정 유효성 검사
try:
    validate_settings()
except Exception as e:
    print(f"Configuration validation failed: {e}")
    raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    # 시작 시
    print("Starting College Notice Crawler...")

    # 데이터베이스 초기화
    if init_database():
        print("Database initialized successfully")
    else:
        print("Database initialization failed")

    # 대학 크롤링 스케줄러 초기화
    if init_college_scheduler():
        print("College scheduler initialized successfully")
    else:
        print("College scheduler initialization failed")

    print("Application startup completed")

    yield

    # 종료 시
    print("Shutting down College Notice Crawler...")


# FastAPI 앱 생성
app = FastAPI(
    title="인천대학교 공지사항 크롤링 서버 API",
    description="인천대학교 공지사항을 자동으로 수집하고 관리하는 크롤링 시스템",
    version="1.0.0",
    lifespan=lifespan,
)

# 보안 미들웨어 설정
from middleware.security import add_security_headers, RateLimiter, IPBlocker, verify_api_key
from middleware import MetricsMiddleware
from metrics import metrics_endpoint

# Metrics 미들웨어 (가장 먼저 추가)
app.add_middleware(MetricsMiddleware)

# CORS 미들웨어
allowed_origins = os.getenv("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8000").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "X-API-Key"],
)

# 신뢰할 수 있는 호스트 미들웨어
allowed_hosts = os.getenv("ALLOWED_HOSTS", "localhost,127.0.0.1,fastapi,*").split(",")
app.add_middleware(TrustedHostMiddleware, allowed_hosts=allowed_hosts)

# 보안 헤더 미들웨어
app.middleware("http")(add_security_headers)

# 레이트 리미팅 설정
rate_limiter = RateLimiter(
    requests_per_minute=int(os.getenv("MAX_REQUESTS_PER_MINUTE", "60"))
)

# IP 차단 설정
ip_blocker = IPBlocker()


@app.middleware("http")
async def security_middleware(request: Request, call_next):
    """보안 미들웨어"""
    client_ip = request.client.host

    # IP 차단 확인
    if ip_blocker.is_ip_blocked(client_ip):
        raise HTTPException(
            status_code=403,
            detail="Your IP has been blocked due to suspicious activity"
        )

    # 레이트 리미팅 확인
    if not await rate_limiter.check_rate_limit(client_ip):
        raise HTTPException(
            status_code=429,
            detail="Too many requests"
        )

    return await call_next(request)

@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    """요청 처리 시간 측정 및 로깅"""
    request_id = str(uuid.uuid4())
    start_time = time.time()

    # 요청 시작 로깅
    print(f"Request {request_id} started: {request.method} {request.url}")

    response = await call_next(request)

    # 요청 완료 로깅
    process_time = time.time() - start_time
    log_request(
        request_id, request.method, str(request.url), response.status_code, process_time
    )

    response.headers["X-Process-Time"] = str(process_time)
    response.headers["X-Request-ID"] = request_id

    return response


# API 라우터 등록
app.include_router(api_router)


@app.get(
    "/",
    tags=["시스템 정보"],
    summary="API 정보 조회",
    description="크롤링 서버의 기본 정보를 반환합니다."
)
async def root():
    """루트 엔드포인트"""
    return {
        "message": "인천대학교 공지사항 크롤링 서버 API",
        "version": "1.0.0",
        "status": "running",
    }


@app.get(
    "/metrics",
    tags=["모니터링"],
    summary="시스템 메트릭 조회",
    description="Prometheus 형식의 시스템 메트릭을 반환합니다 (HTTP 요청, 크롤러 통계, Circuit Breaker 등)."
)
async def metrics():
    """
    Prometheus 메트릭 엔드포인트

    시스템 메트릭 수집:
    - HTTP 요청/응답
    - 크롤러 실행 통계
    - Circuit Breaker 상태
    - 데이터베이스 쿼리
    """
    return metrics_endpoint()


@app.get(
    "/health",
    tags=["시스템 상태"],
    summary="전체 시스템 헬스 체크",
    description="데이터베이스, Redis, Celery Worker의 상태를 종합적으로 확인합니다."
)
async def health_check():
    """
    헬스 체크 엔드포인트

    시스템 상태 확인:
    - 데이터베이스 연결
    - Redis 연결
    - Celery 워커 상태

    Returns:
        HealthCheckResponse 형식의 응답
    """
    from app.schemas import HealthCheckResponse
    from database import engine
    from celery import Celery
    from config import get_redis_url
    import redis
    from sqlalchemy import text

    # 데이터베이스 연결 확인
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        db_healthy = True
    except Exception as e:
        print(f"Database health check failed: {e}")
        db_healthy = False

    # Redis 연결 확인
    try:
        redis_url = get_redis_url()
        r = redis.from_url(redis_url)
        r.ping()
        redis_healthy = True
    except Exception:
        redis_healthy = False

    # Celery 워커 수 확인
    try:
        celery_app = Celery(broker=get_redis_url())
        stats = celery_app.control.inspect().stats()
        worker_count = len(stats) if stats else 0
    except Exception:
        worker_count = 0

    return {
        "status": "healthy" if (db_healthy and redis_healthy) else "degraded",
        "timestamp": datetime.now(),
        "version": "1.0.0",
        "database": db_healthy,
        "redis": redis_healthy,
        "celery_workers": worker_count
    }


@app.get(
    "/test-crawlers",
    tags=["크롤러 실행"],
    summary="크롤러 실시간 테스트",
    description="실제 웹사이트에 접속하여 크롤러가 정상 동작하는지 테스트합니다."
)
async def test_crawlers():
    """크롤러 테스트 엔드포인트"""
    from auto_scheduler import get_auto_scheduler

    try:
        scheduler = get_auto_scheduler()
        results = scheduler.test_crawlers()
        return {
            "status": "success",
            "message": "Crawler test completed",
            "results": results,
        }
    except Exception as e:
        return {"status": "error", "message": f"Crawler test failed: {str(e)}"}


@app.post(
    "/run-crawler/{category}",
    tags=["크롤러 실행"],
    summary="카테고리별 크롤러 수동 실행",
    description="특정 카테고리 또는 전체 카테고리의 크롤러를 즉시 실행합니다 (API Key 필요)."
)
async def run_crawler(
    category: str,
    api_key: str = Depends(verify_api_key)
):
    """
    특정 카테고리 크롤러 수동 실행 (Celery 태스크 트리거)

    - **category**: 크롤링 카테고리 (volunteer, job, scholarship, etc.) 또는 'all'
    - 인증 필요: X-API-Key 헤더

    Returns:
        CrawlerTriggerResponse 형식의 응답
    """
    from tasks import college_crawl_task
    from database import get_db_context
    from crud import get_jobs
    from app.schemas import CrawlerTriggerResponse

    try:
        # 카테고리에 해당하는 job 찾기
        with get_db_context() as db:
            jobs = get_jobs(db)

            # 카테고리별 job 이름 매핑
            category_names = {
                "volunteer": "봉사 공지사항 크롤링",
                "job": "취업 공지사항 크롤링",
                "scholarship": "장학금 공지사항 크롤링",
                "general_events": "일반행사/채용 크롤링",
                "educational_test": "교육시험 크롤링",
                "tuition_payment": "등록금납부 크롤링",
                "academic_credit": "학점 크롤링",
                "degree": "학위 크롤링",
            }

            triggered_tasks = []

            if category == "all":
                # 모든 카테고리 실행
                for job in jobs:
                    task = college_crawl_task.delay(job.name)
                    triggered_tasks.append({
                        "job_name": job.name,
                        "job_id": job.id,
                        "task_id": task.id
                    })
            else:
                # 특정 카테고리만 실행
                job_name = category_names.get(category)
                if not job_name:
                    return {"status": "error", "message": f"Unknown category: {category}"}

                target_job = next((j for j in jobs if j.name == job_name), None)
                if not target_job:
                    return {"status": "error", "message": f"Job not found for category: {category}"}

                task = college_crawl_task.delay(target_job.name)
                triggered_tasks.append({
                    "job_name": target_job.name,
                    "job_id": target_job.id,
                    "task_id": task.id
                })

            return {
                "status": "success",
                "category": category,
                "message": "Crawling tasks triggered",
                "tasks": triggered_tasks,
                "count": len(triggered_tasks)
            }

    except Exception as e:
        return {"status": "error", "message": f"Crawler execution failed: {str(e)}"}


@app.post(
    "/force-schedule-update",
    tags=["크롤러 실행"],
    summary="스케줄 강제 업데이트",
    description="Celery Beat 스케줄을 데이터베이스 정보와 동기화합니다."
)
async def force_schedule_update():
    """Celery 스케줄 강제 업데이트"""
    from auto_scheduler import get_auto_scheduler

    try:
        scheduler = get_auto_scheduler()
        scheduler.update_celery_schedule()
        return {"status": "success", "message": "Schedule updated successfully"}
    except Exception as e:
        return {"status": "error", "message": f"Schedule update failed: {str(e)}"}


@app.get(
    "/test-sentry",
    tags=["시스템 정보"],
    summary="Sentry 연동 테스트",
    description="Sentry와 Slack 연동을 테스트하기 위한 에러를 발생시킵니다."
)
async def test_sentry():
    """Sentry 테스트 엔드포인트"""
    from sentry_config import track_crawler_error, capture_message_with_level
    from slack_notify import send_slack_alert

    # 정보 메시지 전송
    capture_message_with_level(
        "Sentry 연동 테스트: 정보 메시지",
        level="info",
        context={
            "test_type": "info_message",
            "timestamp": datetime.now().isoformat()
        }
    )

    # 크롤러 에러 테스트
    track_crawler_error(
        category="test",
        error_type="TestError",
        url="http://localhost:8001/test-sentry",
        exception=Exception("🧪 Sentry와 Slack 연동 테스트입니다!"),
        extra_data={
            "test_purpose": "Sentry-Slack integration test",
            "expected_result": "Slack notification should appear"
        }
    )

    # Slack 직접 알림 전송
    slack_enabled = os.getenv("ENABLE_SLACK_NOTIFICATIONS", "false").lower() == "true"
    slack_sent = False

    if slack_enabled:
        slack_message = (
            "🧪 *크롤링 서버 테스트 알림*\n\n"
            "✅ Sentry-Slack 연동 테스트가 실행되었습니다!\n\n"
            f"• 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n"
            "• 환경: development\n"
            "• URL: http://localhost:8001/test-sentry\n\n"
            "이 메시지가 보인다면 Slack 알림이 정상적으로 작동하고 있습니다! 🎉"
        )
        slack_sent = send_slack_alert(slack_message)

    return {
        "status": "success",
        "message": "테스트 이벤트를 Sentry로 전송했습니다!",
        "slack_notification": "전송됨" if slack_sent else "비활성화 또는 실패",
        "instructions": [
            "1. Sentry 대시보드(https://sentry.io)에서 이벤트를 확인하세요",
            "2. Slack 채널에서 알림을 확인하세요",
            "3. 알림이 오지 않으면 Alert Rules를 확인하세요"
        ]
    }


@app.get(
    "/test-daily-report",
    tags=["시스템 정보"],
    summary="일일 리포트 테스트",
    description="일일 요약 리포트를 즉시 생성하여 Slack으로 전송합니다."
)
async def test_daily_report():
    """일일 리포트 테스트 엔드포인트"""
    try:
        from tasks import send_daily_report

        # 백그라운드에서 실행
        result = send_daily_report.apply_async()

        return {
            "status": "success",
            "message": "일일 리포트 생성 작업이 시작되었습니다!",
            "task_id": result.id,
            "instructions": [
                "1. Slack 채널에서 리포트를 확인하세요",
                "2. 리포트에는 어제 수집된 문서 통계가 포함됩니다",
                "3. 매일 오전 9시에 자동으로 전송됩니다"
            ]
        }
    except Exception as e:
        logger.error(f"Failed to trigger daily report: {e}")
        return {
            "status": "error",
            "message": f"일일 리포트 생성 실패: {str(e)}"
        }


@app.get(
    "/dashboard",
    tags=["모니터링"],
    summary="웹 대시보드",
    description="크롤링 데이터를 시각화하는 HTML 대시보드를 제공합니다."
)
async def dashboard():
    """크롤링 데이터 대시보드"""
    html_content = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>대학 공지사항 크롤링 대시보드</title>
        <meta charset="utf-8">
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .container { max-width: 1200px; margin: 0 auto; }
            .header { background: #f0f0f0; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
            .section { background: white; padding: 20px; margin-bottom: 20px; border: 1px solid #ddd; border-radius: 5px; }
            .button { background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 3px; cursor: pointer; margin: 5px; }
            .button:hover { background: #0056b3; }
            .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
            .stat-card { background: #f8f9fa; padding: 20px; border-radius: 5px; text-align: center; }
            .stat-number { font-size: 2em; font-weight: bold; color: #007bff; }
            .data-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
            .data-table th, .data-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            .data-table th { background-color: #f2f2f2; }
            .search-box { width: 300px; padding: 8px; margin: 10px 0; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>🏫 대학 공지사항 크롤링 대시보드</h1>
                <p>인천대학교 공지사항 크롤링 현황 및 데이터 조회</p>
            </div>
            
            <div class="section">
                <h2>📊 크롤링 통계</h2>
                <div class="stats">
                    <div class="stat-card">
                        <div class="stat-number" id="total-docs">-</div>
                        <div>총 문서 수</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number" id="total-jobs">-</div>
                        <div>크롤링 작업 수</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number" id="active-jobs">-</div>
                        <div>활성 작업 수</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number" id="latest-update">-</div>
                        <div>최근 업데이트</div>
                    </div>
                </div>
                <button class="button" onclick="loadStats()">통계 새로고침</button>
            </div>
            
            <div class="section">
                <h2>🔍 데이터 검색</h2>
                <input type="text" id="search-query" class="search-box" placeholder="검색어를 입력하세요...">
                <select id="search-source">
                    <option value="">모든 소스</option>
                    <option value="volunteer">봉사</option>
                    <option value="job">취업</option>
                    <option value="scholarship">장학금</option>
                    <option value="general_events">일반행사</option>
                    <option value="educational_test">교육시험</option>
                    <option value="tuition_payment">등록금납부</option>
                    <option value="academic_credit">학점</option>
                    <option value="degree">학위</option>
                </select>
                <button class="button" onclick="searchDocuments()">검색</button>
                <div id="search-results"></div>
            </div>
            
            <div class="section">
                <h2>📋 최근 크롤링된 문서</h2>
                <button class="button" onclick="loadRecentDocuments()">최근 문서 로드</button>
                <div id="recent-documents"></div>
            </div>
            
            <div class="section">
                <h2>⚙️ 크롤링 관리</h2>
                <button class="button" onclick="testCrawlers()">크롤러 테스트</button>
                <button class="button" onclick="updateSchedule()">스케줄 업데이트</button>
                <button class="button" onclick="runAllCrawlers()">전체 크롤링 실행</button>
                <div id="management-results"></div>
            </div>
        </div>
        
        <script>
            // 페이지 로드 시 통계 로드
            window.onload = function() {
                loadStats();
                loadRecentDocuments();
            };
            
            async function loadStats() {
                try {
                    const response = await fetch('/api/v1/documents/summary');
                    const data = await response.json();
                    
                    document.getElementById('total-docs').textContent = data.total_documents || 0;
                    document.getElementById('latest-update').textContent = data.latest_update ? new Date(data.latest_update).toLocaleDateString() : 'N/A';
                    
                    // 작업 상태도 로드
                    const statusResponse = await fetch('/api/v1/crawling-status');
                    const statusData = await statusResponse.json();
                    
                    document.getElementById('total-jobs').textContent = statusData.job_status?.length || 0;
                    document.getElementById('active-jobs').textContent = statusData.job_status?.filter(j => j.status === 'ACTIVE').length || 0;
                    
                } catch (error) {
                    console.error('통계 로드 실패:', error);
                }
            }
            
            async function loadRecentDocuments() {
                try {
                    const response = await fetch('/api/v1/documents/recent?limit=10');
                    const data = await response.json();
                    
                    const container = document.getElementById('recent-documents');
                    if (data.documents && data.documents.length > 0) {
                        let html = '<table class="data-table"><tr><th>제목</th><th>작성자</th><th>카테고리</th><th>소스</th><th>크롤링 시간</th></tr>';
                        data.documents.forEach(doc => {
                            html += `<tr>
                                <td><a href="${doc.url}" target="_blank">${doc.title}</a></td>
                                <td>${doc.writer}</td>
                                <td>${doc.category || 'N/A'}</td>
                                <td>${doc.source}</td>
                                <td>${new Date(doc.created_at).toLocaleString()}</td>
                            </tr>`;
                        });
                        html += '</table>';
                        container.innerHTML = html;
                    } else {
                        container.innerHTML = '<p>아직 크롤링된 문서가 없습니다.</p>';
                    }
                } catch (error) {
                    console.error('최근 문서 로드 실패:', error);
                    document.getElementById('recent-documents').innerHTML = '<p>문서 로드에 실패했습니다.</p>';
                }
            }
            
            async function searchDocuments() {
                const query = document.getElementById('search-query').value;
                const source = document.getElementById('search-source').value;
                
                if (!query) {
                    alert('검색어를 입력해주세요.');
                    return;
                }
                
                try {
                    let url = `/api/v1/documents/search?q=${encodeURIComponent(query)}`;
                    if (source) {
                        url += `&source=${encodeURIComponent(source)}`;
                    }
                    
                    const response = await fetch(url);
                    const data = await response.json();
                    
                    const container = document.getElementById('search-results');
                    if (data.results && data.results.length > 0) {
                        let html = `<h3>검색 결과 (${data.total_found}개)</h3>`;
                        html += '<table class="data-table"><tr><th>제목</th><th>작성자</th><th>카테고리</th><th>소스</th><th>크롤링 시간</th></tr>';
                        data.results.forEach(doc => {
                            html += `<tr>
                                <td><a href="${doc.url}" target="_blank">${doc.title}</a></td>
                                <td>${doc.writer}</td>
                                <td>${doc.category || 'N/A'}</td>
                                <td>${doc.source}</td>
                                <td>${new Date(doc.created_at).toLocaleString()}</td>
                            </tr>`;
                        });
                        html += '</table>';
                        container.innerHTML = html;
                    } else {
                        container.innerHTML = '<p>검색 결과가 없습니다.</p>';
                    }
                } catch (error) {
                    console.error('검색 실패:', error);
                    document.getElementById('search-results').innerHTML = '<p>검색에 실패했습니다.</p>';
                }
            }
            
            async function testCrawlers() {
                try {
                    const response = await fetch('/test-crawlers');
                    const data = await response.json();
                    
                    document.getElementById('management-results').innerHTML = 
                        `<h3>크롤러 테스트 결과</h3><pre>${JSON.stringify(data, null, 2)}</pre>`;
                } catch (error) {
                    document.getElementById('management-results').innerHTML = 
                        `<p>크롤러 테스트 실패: ${error.message}</p>`;
                }
            }
            
            async function updateSchedule() {
                try {
                    const response = await fetch('/force-schedule-update', {method: 'POST'});
                    const data = await response.json();
                    
                    document.getElementById('management-results').innerHTML = 
                        `<h3>스케줄 업데이트 결과</h3><pre>${JSON.stringify(data, null, 2)}</pre>`;
                } catch (error) {
                    document.getElementById('management-results').innerHTML = 
                        `<p>스케줄 업데이트 실패: ${error.message}</p>`;
                }
            }
            
            async function runAllCrawlers() {
                // API 키 입력 받기
                const apiKey = prompt('API 키를 입력하세요:');
                if (!apiKey) {
                    alert('API 키가 필요합니다.');
                    return;
                }

                try {
                    const response = await fetch('/run-crawler/all', {
                        method: 'POST',
                        headers: {
                            'X-API-Key': apiKey
                        }
                    });
                    const data = await response.json();
                    
                    document.getElementById('management-results').innerHTML =
                        `<h3>전체 크롤링 실행 결과</h3>
                        <p>백그라운드에서 크롤링 작업이 시작되었습니다. 약 10초 후 자동으로 통계가 업데이트됩니다.</p>
                        <pre>${JSON.stringify(data, null, 2)}</pre>`;

                    // 크롤링 완료를 위해 충분한 시간 대기 후 새로고침
                    setTimeout(() => {
                        loadStats();
                        loadRecentDocuments();
                        document.getElementById('management-results').innerHTML +=
                            '<p style="color: green;">✓ 통계와 최근 문서가 업데이트되었습니다.</p>';
                    }, 10000);
                } catch (error) {
                    document.getElementById('management-results').innerHTML = 
                        `<p>전체 크롤링 실행 실패: ${error.message}</p>`;
                }
            }
        </script>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)
