# tasks.py
import os
import time
import uuid
from celery import Celery
from datetime import timedelta
from celery.schedules import crontab
import hashlib
import logging

from config import get_redis_url, get_crawler_config
from database import get_db_context
from models import CrawlNotice
from crud import create_task, update_task_status, increment_task_retries, create_document, bulk_create_documents, get_job_by_name, get_document_by_url
from robots_parser import RobotsManager
from rate_limiter import get_rate_limiter
from url_utils import get_duplicate_checker, get_url_normalizer
from logging_config import log_crawler_event, log_error, log_performance
from college_crawlers import get_college_crawler
import json

# 로거 설정
logger = logging.getLogger(__name__)

# Redis 설정
BROKER_URL = get_redis_url()
RESULT_BACKEND = get_redis_url()

celery_app = Celery(
    "school_notices",
    broker=BROKER_URL,
    backend=RESULT_BACKEND,
)

# Celery 설정
celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="Asia/Seoul",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=30 * 60,  # 30분
    task_soft_time_limit=25 * 60,  # 25분
    worker_prefetch_multiplier=1,
    worker_max_tasks_per_child=1000,
)

# 전역 인스턴스들
robots_manager = RobotsManager()
rate_limiter = get_rate_limiter()
duplicate_checker = get_duplicate_checker()
url_normalizer = get_url_normalizer()
college_crawler = get_college_crawler()


# 우선순위 큐: priority (P0~P3)
PRIORITY_MAP = {"P0": 0, "P1": 1, "P2": 2, "P3": 3}


@celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
def college_crawl_task(self, job_name: str):
    """대학 공지사항 크롤링 태스크"""
    request_id = str(uuid.uuid4())
    start_time = time.time()

    try:
        logger.info(f"Starting college crawl task {request_id} for job: {job_name}")
        log_crawler_event("START", job_name, "PENDING", f"request_id={request_id}")

        # 대학 크롤러로 크롤링 실행
        if job_name == "봉사 공지사항 크롤링":
            results = college_crawler.crawl_volunteer()
        elif job_name == "취업 공지사항 크롤링":
            results = college_crawler.crawl_job()
        elif job_name == "장학금 공지사항 크롤링":
            results = college_crawler.crawl_scholarship()
        else:
            # 통합 크롤링
            results = college_crawler.crawl_all()

        # 결과 처리 및 데이터베이스 저장
        total_items = 0
        saved_items = 0
        skipped_items = 0

        # 잡 ID 조회
        with get_db_context() as db:
            db_job = get_job_by_name(db, job_name)
            if not db_job:
                logger.warning(f"Job not found: {job_name}")
                job_id = None
            else:
                job_id = db_job.id

        # 결과를 데이터베이스에 저장 (벌크 삽입 최적화)
        if isinstance(results, list):
            total_items = len(results)
            if job_id:
                # 1단계: 중복 체크 (한 번에 수행)
                docs_to_insert = []

                with get_db_context() as db:
                    # 모든 URL을 한 번에 조회
                    urls_to_check = [item.get('url', '') for item in results if item.get('url')]
                    existing_urls = set()

                    if urls_to_check:
                        existing_docs = db.query(CrawlNotice).filter(CrawlNotice.url.in_(urls_to_check)).all()
                        existing_urls = {doc.url for doc in existing_docs}

                    # 2단계: 삽입할 데이터 준비
                    for item in results:
                        try:
                            item_url = item.get('url', '')

                            # 중복 체크
                            if item_url in existing_urls:
                                logger.debug(f"Document already exists: {item_url}")
                                skipped_items += 1
                                continue

                            # fingerprint 생성
                            fingerprint_str = f"{item_url}_{item.get('title', '')}"
                            fingerprint = hashlib.sha256(fingerprint_str.encode()).hexdigest()

                            # 문서 데이터 생성
                            doc_data = {
                                "job_id": job_id,
                                "url": item_url,
                                "title": item.get('title'),
                                "writer": item.get('writer'),
                                "date": item.get('date'),
                                "hits": item.get('hits'),
                                "category": item.get('category'),
                                "source": item.get('source'),
                                "extracted": item,
                                "raw": json.dumps(item, ensure_ascii=False),
                                "fingerprint": fingerprint,
                                "snapshot_version": "v1"
                            }

                            docs_to_insert.append(doc_data)
                        except Exception as e:
                            logger.error(f"Failed to prepare document: {e}")
                            continue

                    # 3단계: 벌크 삽입
                    if docs_to_insert:
                        try:
                            saved_count = bulk_create_documents(db, docs_to_insert)
                            saved_items = saved_count
                            logger.info(f"Bulk inserted {saved_count} documents")
                        except Exception as e:
                            logger.error(f"Bulk insert failed: {e}")
                            # 폴백: 개별 삽입
                            for doc_data in docs_to_insert:
                                try:
                                    create_document(db, doc_data)
                                    saved_items += 1
                                except Exception as doc_error:
                                    logger.error(f"Failed to save document individually: {doc_error}")
                                    continue
        elif isinstance(results, dict):
            total_items = sum(len(items) for items in results.values())
            if job_id:
                # 모든 카테고리의 아이템을 하나의 리스트로 통합
                all_items = []
                for category, items in results.items():
                    all_items.extend(items)

                # 1단계: 중복 체크 (한 번에 수행)
                docs_to_insert = []

                with get_db_context() as db:
                    # 모든 URL을 한 번에 조회
                    urls_to_check = [item.get('url', '') for item in all_items if item.get('url')]
                    existing_urls = set()

                    if urls_to_check:
                        existing_docs = db.query(CrawlNotice).filter(CrawlNotice.url.in_(urls_to_check)).all()
                        existing_urls = {doc.url for doc in existing_docs}

                    # 2단계: 삽입할 데이터 준비
                    for item in all_items:
                        try:
                            item_url = item.get('url', '')

                            # 중복 체크
                            if item_url in existing_urls:
                                logger.debug(f"Document already exists: {item_url}")
                                skipped_items += 1
                                continue

                            # fingerprint 생성
                            fingerprint_str = f"{item_url}_{item.get('title', '')}"
                            fingerprint = hashlib.sha256(fingerprint_str.encode()).hexdigest()

                            # 문서 데이터 생성
                            doc_data = {
                                "job_id": job_id,
                                "url": item_url,
                                "title": item.get('title'),
                                "writer": item.get('writer'),
                                "date": item.get('date'),
                                "hits": item.get('hits'),
                                "category": item.get('category'),
                                "source": item.get('source'),
                                "extracted": item,
                                "raw": json.dumps(item, ensure_ascii=False),
                                "fingerprint": fingerprint,
                                "snapshot_version": "v1"
                            }

                            docs_to_insert.append(doc_data)
                        except Exception as e:
                            logger.error(f"Failed to prepare document: {e}")
                            continue

                    # 3단계: 벌크 삽입
                    if docs_to_insert:
                        try:
                            saved_count = bulk_create_documents(db, docs_to_insert)
                            saved_items = saved_count
                            logger.info(f"Bulk inserted {saved_count} documents")
                        except Exception as e:
                            logger.error(f"Bulk insert failed: {e}")
                            # 폴백: 개별 삽입
                            for doc_data in docs_to_insert:
                                try:
                                    create_document(db, doc_data)
                                    saved_items += 1
                                except Exception as doc_error:
                                    logger.error(f"Failed to save document individually: {doc_error}")
                                    continue

        # 성능 로깅
        duration = time.time() - start_time
        log_performance(
            "college_crawl_task",
            duration,
            {"job_name": job_name, "total_items": total_items, "saved_items": saved_items, "skipped_items": skipped_items},
        )

        logger.info(
            f"College crawl task {request_id} completed successfully in {duration:.2f}s - Saved: {saved_items}, Skipped: {skipped_items}"
        )
        log_crawler_event(
            "COMPLETE",
            job_name,
            "SUCCESS",
            f"duration={duration:.2f}s, items={total_items}, saved={saved_items}, skipped={skipped_items}",
        )

        return {
            "status": "success",
            "job_name": job_name,
            "total_items": total_items,
            "saved_items": saved_items,
            "skipped_items": skipped_items,
            "duration": duration,
        }

    except Exception as exc:
        duration = time.time() - start_time
        error_msg = str(exc)

        logger.error(f"College crawl task {request_id} failed: {error_msg}")
        log_error(
            exc,
            f"college_crawl_task for {job_name}",
            {
                "job_name": job_name,
                "duration": duration,
            },
        )

        # 재시도 로직
        try:
            self.retry(exc=exc, countdown=(2**self.request.retries) + 5)
        except self.MaxRetriesExceededError:
            log_crawler_event("MAX_RETRIES", job_name, "FAILED", f"error={error_msg}")
            return {"status": "failed", "error": error_msg, "job_name": job_name}


@celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
def crawl_task(
    self, job_id: int, url: str, priority: str = "P2", render_mode: str = "STATIC"
):
    """크롤링 태스크 실행"""
    request_id = str(uuid.uuid4())
    start_time = time.time()

    try:
        logger.info(f"Starting crawl task {request_id} for URL: {url}")
        log_crawler_event(
            "START", url, "PENDING", f"job_id={job_id}, priority={priority}"
        )

        # 1. 중복 체크
        if duplicate_checker.is_duplicate(url):
            logger.info(f"URL already crawled: {url}")
            log_crawler_event("DUPLICATE", url, "SKIPPED", f"job_id={job_id}")
            return {"status": "skipped", "reason": "duplicate", "url": url}

        # 2. robots.txt 준수 확인
        crawler_config = get_crawler_config()
        if crawler_config.get("respect_robots_txt", True):
            if not robots_manager.is_allowed(url, crawler_config.get("user_agent")):
                logger.warning(f"URL blocked by robots.txt: {url}")
                log_crawler_event("ROBOTS_BLOCKED", url, "BLOCKED", f"job_id={job_id}")
                return {"status": "blocked", "reason": "robots_txt", "url": url}

            # crawl-delay 준수
            robots_manager.wait_if_needed(url, crawler_config.get("user_agent"))

        # 3. 레이트 리미팅 확인
        if not rate_limiter.can_make_request(url):
            logger.info(f"Rate limit reached for host, waiting...")
            if not rate_limiter.wait_for_request(url, timeout=60):
                raise Exception("Rate limit timeout exceeded")

        # 4. 데이터베이스에 태스크 상태 업데이트
        with get_db_context() as db:
            db_task = create_task(
                db, {"job_id": job_id, "url": url, "status": "RUNNING"}
            )
            task_id = db_task.id

        # 5. 실제 크롤링 실행
        result = execute_crawling(url, render_mode, request_id)

        # 6. 결과 저장 및 상태 업데이트
        with get_db_context() as db:
            update_task_status(
                db,
                task_id,
                "SUCCESS",
                http_status=result.get("http_status"),
                content_hash=result.get("content_hash"),
            )

        # 7. 중복 체크에 URL 추가
        duplicate_checker.mark_as_seen(url)

        # 8. 성능 로깅
        duration = time.time() - start_time
        log_performance(
            "crawl_task",
            duration,
            {"url": url, "render_mode": render_mode, "priority": priority},
        )

        logger.info(
            f"Crawl task {request_id} completed successfully in {duration:.2f}s"
        )
        log_crawler_event("COMPLETE", url, "SUCCESS", f"duration={duration:.2f}s")

        return result

    except Exception as exc:
        duration = time.time() - start_time
        error_msg = str(exc)

        logger.error(f"Crawl task {request_id} failed: {error_msg}")
        log_error(
            exc,
            f"crawl_task for {url}",
            {
                "job_id": job_id,
                "priority": priority,
                "render_mode": render_mode,
                "duration": duration,
            },
        )

        # 데이터베이스 상태 업데이트
        try:
            with get_db_context() as db:
                if "task_id" in locals():
                    update_task_status(db, task_id, "FAILED", error=error_msg)
        except Exception as db_error:
            logger.error(f"Failed to update task status in database: {db_error}")

        # 재시도 로직
        try:
            self.retry(exc=exc, countdown=(2**self.request.retries) + 5)
        except self.MaxRetriesExceededError:
            log_crawler_event("MAX_RETRIES", url, "FAILED", f"error={error_msg}")
            return {"status": "failed", "error": error_msg, "url": url}


def execute_crawling(url: str, render_mode: str, request_id: str) -> dict:
    """실제 크롤링 실행"""
    start_time = time.time()

    try:
        if render_mode == "STATIC":
            result = crawl_static(url, request_id)
        elif render_mode == "HEADLESS":
            result = crawl_headless(url, request_id)
        elif render_mode == "AUTO":
            # 정적 먼저 시도, 실패 시 헤드리스로 전환
            try:
                result = crawl_static(url, request_id)
            except Exception as static_error:
                logger.info(f"Static crawling failed, trying headless: {static_error}")
                result = crawl_headless(url, request_id)
        else:
            raise ValueError(f"Unknown render mode: {render_mode}")

        # 브라우저 사용 시간 기록
        browser_time = int((time.time() - start_time) * 1000)  # 밀리초
        result["cost_ms_browser"] = browser_time

        # 레이트 리미터에 요청 완료 기록
        rate_limiter.record_request_end(url, request_id, browser_time)

        return result

    except Exception as e:
        rate_limiter.record_request_end(url, request_id)
        raise e


def crawl_static(url: str, request_id: str) -> dict:
    """정적 크롤링 (requests 사용)"""
    import requests
    from config import get_crawler_config

    config = get_crawler_config()
    headers = {"User-Agent": config.get("user_agent")}

    response = requests.get(
        url,
        headers=headers,
        timeout=config.get("request_timeout", 30),
        stream=True,  # 대용량 파일 처리
    )

    # 응답 크기 확인
    content_length = response.headers.get("content-length")
    if content_length and int(content_length) > config.get(
        "max_content_length", 10 * 1024 * 1024
    ):
        raise ValueError(f"Content too large: {content_length} bytes")

    response.raise_for_status()

    # 내용 읽기
    content = response.content.decode("utf-8", errors="ignore")

    # 간단한 파싱 (예시)
    extracted_data = parse_static_content(content, url)

    return {
        "url": url,
        "status": "success",
        "http_status": response.status_code,
        "content_hash": hashlib.sha256(content.encode()).hexdigest(),
        "data": extracted_data,
        "render_mode": "STATIC",
    }


def crawl_headless(url: str, request_id: str) -> dict:
    """헤드리스 브라우저 크롤링 (Playwright 사용)"""
    from playwright_crawler import crawl_with_headless
    from config import get_playwright_config

    config = get_playwright_config()

    # Playwright로 크롤링
    html_content = crawl_with_headless(url, config)

    # 간단한 파싱 (예시)
    extracted_data = parse_static_content(html_content, url)

    return {
        "url": url,
        "status": "success",
        "http_status": 200,
        "content_hash": hashlib.sha256(html_content.encode()).hexdigest(),
        "data": extracted_data,
        "render_mode": "HEADLESS",
    }


def parse_static_content(content: str, url: str) -> dict:
    """정적 콘텐츠 파싱 (기본 구현)"""
    # TODO: 실제 파싱 로직 구현
    # 현재는 기본 정보만 추출

    import re

    # 제목 추출 (간단한 정규식)
    title_match = re.search(r"<title[^>]*>([^<]+)</title>", content, re.IGNORECASE)
    title = title_match.group(1).strip() if title_match else "No title"

    # 링크 추출
    links = re.findall(r'href=["\']([^"\']+)["\']', content)

    # 텍스트 길이
    text_content = re.sub(r"<[^>]+>", "", content)
    text_length = len(text_content.strip())

    return {
        "title": title,
        "links_count": len(links),
        "text_length": text_length,
        "url": url,
    }


# Beat 스케줄러 등록
celery_app.conf.beat_schedule = {
    "refresh-job-1": {
        "task": "tasks.crawl_task",
        "schedule": crontab(minute="*/15"),  # 15분마다
        "args": (1, "https://example.com/sitemap.xml", "P1", "AUTO"),
        "options": {"priority": PRIORITY_MAP["P1"]},
    },
    # 추가 잡/스케줄 등록 가능
}

# 리프레시/데드레터 큐/우선순위 큐 등은 celery 설정에서 routing/queue로 확장 가능
