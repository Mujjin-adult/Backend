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
# from database import get_db_context
# from crud import create_task, update_task_status, increment_task_retries
from robots_parser import RobotsManager
from rate_limiter import get_rate_limiter
from url_utils import get_duplicate_checker, get_url_normalizer
from logging_config import log_crawler_event, log_error, log_performance
from college_crawlers import get_college_crawler

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
def run_college_crawler(self, category: str):
    """대학 공지사항 크롤링 태스크 (카테고리 기반)"""
    request_id = str(uuid.uuid4())
    start_time = time.time()

    try:
        logger.info(f"Starting college crawl task {request_id} for category: {category}")
        log_crawler_event("START", category, "PENDING", f"request_id={request_id}")

        # 카테고리별 크롤링 실행
        category_methods = {
            "volunteer": college_crawler.crawl_volunteer,
            "job": college_crawler.crawl_job,
            "scholarship": college_crawler.crawl_scholarship,
            "general_events": college_crawler.crawl_general_events,
            "educational_test": college_crawler.crawl_educational_test,
            "tuition_payment": college_crawler.crawl_tuition_payment,
            "academic_credit": college_crawler.crawl_academic_credit,
            "degree": college_crawler.crawl_degree,
            "all": college_crawler.crawl_all,
        }

        if category not in category_methods:
            raise ValueError(f"Unknown category: {category}")

        results = category_methods[category]()

        # 결과 처리
        total_items = 0
        if isinstance(results, list):
            total_items = len(results)
        elif isinstance(results, dict):
            total_items = sum(len(items) for items in results.values())

        # 성능 로깅
        duration = time.time() - start_time
        log_performance(
            "run_college_crawler",
            duration,
            {"category": category, "total_items": total_items},
        )

        logger.info(
            f"College crawl task {request_id} completed successfully in {duration:.2f}s"
        )
        log_crawler_event(
            "COMPLETE",
            category,
            "SUCCESS",
            f"duration={duration:.2f}s, items={total_items}",
        )

        return {
            "status": "success",
            "category": category,
            "total_items": total_items,
            "duration": duration,
            "results": results,
        }

    except Exception as exc:
        duration = time.time() - start_time
        error_msg = str(exc)

        logger.error(f"College crawl task {request_id} failed: {error_msg}")
        log_error(
            exc,
            f"run_college_crawler for {category}",
            {
                "category": category,
                "duration": duration,
            },
        )

        # 재시도 로직
        try:
            self.retry(exc=exc, countdown=(2**self.request.retries) + 5)
        except self.MaxRetriesExceededError:
            log_crawler_event("MAX_RETRIES", category, "FAILED", f"error={error_msg}")
            return {"status": "failed", "error": error_msg, "category": category}


# Generic crawl_task commented out as it requires database module
# which is not needed for college crawling
# @celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
# def crawl_task(
#     self, job_id: int, url: str, priority: str = "P2", render_mode: str = "STATIC"
# ):
#     """크롤링 태스크 실행"""
#     pass


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


# Beat scheduler will be configured by auto_scheduler.py
# Default schedule disabled as we're using dynamic scheduling
# celery_app.conf.beat_schedule = {}

# 리프레시/데드레터 큐/우선순위 큐 등은 celery 설정에서 routing/queue로 확장 가능
