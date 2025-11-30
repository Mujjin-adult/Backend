"""
대학 공지사항 크롤러 통합 모듈
college_code 폴더의 개별 크롤러들을 통합하여 관리
"""
import requests
from bs4 import BeautifulSoup
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime
import time
import random

from app.circuit_breaker import get_circuit_breaker, CircuitBreakerError
from app.sentry_config import track_crawler_error
from app.content_crawler import get_content_crawler

logger = logging.getLogger(__name__)


# HTTP 에러 분류
class CrawlerHTTPError(Exception):
    """크롤러 HTTP 에러 기본 클래스"""
    pass


class TemporaryError(CrawlerHTTPError):
    """일시적 에러 (재시도 가능)"""
    pass


class PermanentError(CrawlerHTTPError):
    """영구적 에러 (재시도 불가)"""
    pass
def clean_category(category_text: str) -> str:
    """카테고리 텍스트에서 [] 제거"""
    if not category_text:
        return category_text
    return category_text.replace('[', '').replace(']', '')
class CollegeCrawler:
    """대학 공지사항 통합 크롤러"""
    def __init__(self, max_pages: int = 5, max_retries: int = 3, crawl_content: bool = True):
        self.base_url = "https://www.inu.ac.kr"
        self.max_pages = max_pages  # 최대 크롤링 페이지 수
        self.max_retries = max_retries  # 최대 재시도 횟수
        self.crawl_content = crawl_content  # 본문 크롤링 여부
        self.session = requests.Session()
        self.session.headers.update(
            {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            }
        )

        # Circuit Breaker 초기화
        self.circuit_breaker = get_circuit_breaker(
            name="inu_crawler",
            failure_threshold=5,
            success_threshold=2,
            timeout=60.0,
            expected_exception=TemporaryError
        )

        # Content Crawler 초기화 (본문 크롤링용)
        if self.crawl_content:
            self.content_crawler = get_content_crawler()

    def _make_request_with_retry(self, url: str, payload: dict, retry_count: int = 0) -> requests.Response:
        """
        재시도 로직이 포함된 HTTP 요청

        Args:
            url: 요청 URL
            payload: POST 데이터
            retry_count: 현재 재시도 횟수

        Returns:
            Response 객체

        Raises:
            TemporaryError: 일시적 에러 (재시도 가능)
            PermanentError: 영구적 에러 (재시도 불가)
        """
        try:
            def make_request():
                response = self.session.post(url, data=payload, timeout=30)

                # HTTP 상태 코드에 따른 에러 분류
                if response.status_code == 429:  # Too Many Requests
                    raise TemporaryError(f"Rate limited (429): {url}")
                elif response.status_code in [500, 502, 503, 504]:  # Server errors
                    raise TemporaryError(f"Server error ({response.status_code}): {url}")
                elif response.status_code in [400, 401, 403, 404]:  # Client errors
                    raise PermanentError(f"Client error ({response.status_code}): {url}")
                elif response.status_code >= 400:
                    raise TemporaryError(f"HTTP error ({response.status_code}): {url}")

                response.raise_for_status()
                return response

            # Circuit Breaker로 보호된 요청
            return self.circuit_breaker.call(make_request)

        except CircuitBreakerError as e:
            logger.error(f"Circuit breaker is OPEN, skipping request to {url}")
            raise TemporaryError(f"Circuit breaker open: {str(e)}")

        except TemporaryError as e:
            # 일시적 에러는 재시도
            if retry_count < self.max_retries:
                wait_time = (2 ** retry_count) + random.uniform(1, 3)
                logger.warning(
                    f"Temporary error, retrying in {wait_time:.1f}s "
                    f"(attempt {retry_count + 1}/{self.max_retries}): {str(e)}"
                )
                time.sleep(wait_time)
                return self._make_request_with_retry(url, payload, retry_count + 1)
            else:
                logger.error(f"Max retries exceeded for {url}")
                raise

        except PermanentError:
            # 영구적 에러는 재시도하지 않음
            logger.error(f"Permanent error, not retrying: {url}")
            raise

        except requests.exceptions.Timeout as e:
            # 타임아웃은 일시적 에러로 간주
            raise TemporaryError(f"Request timeout: {str(e)}")

        except requests.exceptions.ConnectionError as e:
            # 연결 에러는 일시적 에러로 간주
            raise TemporaryError(f"Connection error: {str(e)}")

        except Exception as e:
            # 기타 예외는 일시적 에러로 간주 (보수적)
            logger.error(f"Unexpected error: {type(e).__name__}: {str(e)}")
            raise TemporaryError(f"Unexpected error: {str(e)}")

    def _handle_crawler_exception(
        self,
        exception: Exception,
        source: str,
        url: str,
        page: int,
        page_num: str = None,
        should_break: bool = True
    ) -> bool:
        """
        크롤러 예외 처리 (공통 로직)

        Args:
            exception: 발생한 예외
            source: 크롤링 소스 (volunteer, job, etc.)
            url: 크롤링 URL
            page: 현재 페이지 번호
            page_num: 페이지 메뉴 번호
            should_break: 루프 중단 여부

        Returns:
            bool: 루프를 중단해야 하면 True, 계속하면 False
        """
        extra_data = {"page": page}
        if page_num:
            extra_data["page_num"] = page_num

        if isinstance(exception, PermanentError):
            logger.error(f"Permanent error on page {page}, stopping: {exception}")
            track_crawler_error(
                category=source,
                error_type="PermanentError",
                url=url,
                exception=exception,
                extra_data=extra_data
            )
            return True  # 항상 중단

        elif isinstance(exception, TemporaryError):
            logger.warning(f"Temporary error on page {page}, skipping: {exception}")
            track_crawler_error(
                category=source,
                error_type="TemporaryError",
                url=url,
                exception=exception,
                extra_data=extra_data
            )
            return False  # 계속 진행

        else:
            logger.error(f"Unexpected error on page {page}: {type(exception).__name__}: {exception}")
            track_crawler_error(
                category=source,
                error_type="UnexpectedError",
                url=url,
                exception=exception,
                extra_data=extra_data
            )
            return should_break

    def _parse_row(self, row, default_category: Optional[str], source: str) -> Optional[Dict[str, Any]]:
        """테이블 행 파싱 (공통 로직)"""
        try:
            a = row.select_one("a")
            if not a:
                return None
            title = a.get_text(strip=True)
            href = a.get("href", "")
            full_url = f"{self.base_url}{href}" if href.startswith("/") else href
            writer = row.select_one("td.td-write")
            writer_text = writer.get_text(strip=True) if writer else "N/A"
            date = row.select_one("td.td-date")
            date_text = date.get_text(strip=True) if date else "N/A"
            hits = row.select_one("td.td-access")
            hits_text = hits.get_text(strip=True) if hits else "N/A"
            # 카테고리 처리
            category_elem = row.select_one("td.td-category")
            if category_elem:
                category_text = category_elem.get_text(strip=True)
                category_text = clean_category(category_text)
            else:
                category_text = default_category or "N/A"

            # 기본 정보
            item = {
                "title": title,
                "writer": writer_text,
                "date": date_text,
                "hits": hits_text,
                "url": full_url,
                "category": category_text,
                "source": source,
            }

            # 본문 크롤링 (옵션)
            if self.crawl_content and self.content_crawler:
                try:
                    logger.debug(f"Crawling content for: {full_url}")
                    content_result = self.content_crawler.crawl_content(
                        url=full_url,
                        category=source
                    )

                    if content_result.get("success"):
                        item["content"] = content_result.get("content", "")
                        logger.debug(f"Successfully crawled content for: {full_url}")
                    else:
                        logger.warning(f"Failed to crawl content for {full_url}: {content_result.get('error')}")
                        item["content"] = ""

                    # 레이트 리미팅을 위한 짧은 딜레이
                    time.sleep(0.3)

                except Exception as content_error:
                    logger.error(f"Error crawling content for {full_url}: {content_error}")
                    item["content"] = ""
            else:
                item["content"] = ""

            return item
        except Exception as e:
            logger.error(f"Error parsing row: {e}")
            return None
    def _crawl_generic(
        self,
        page_num: str,
        category: str,
        source: str,
        max_pages: Optional[int] = None,
        selector: str = "table.board-table tbody tr"
    ) -> List[Dict[str, Any]]:
        """제네릭 크롤링 메서드 (다중 페이지 지원)"""
        url = f"{self.base_url}/bbs/inu/{page_num}/artclList.do"
        max_pages = max_pages or self.max_pages
        all_results = []

        for page in range(1, max_pages + 1):
            try:
                payload = {
                    "layout": "Q0UK5PJj2ZsQo0aX2Frag0Mw8X0X3D",
                    "menuNo": page_num,
                    "page": str(page),
                }

                # 재시도 로직이 포함된 요청
                response = self._make_request_with_retry(url, payload)

                soup = BeautifulSoup(response.text, "html.parser")
                rows = soup.select(selector)

                # 빈 페이지면 종료
                if not rows:
                    logger.info(f"No more data at page {page}, stopping")
                    break

                page_results = []
                for row in rows:
                    item = self._parse_row(row, category, source)
                    if item:
                        page_results.append(item)

                all_results.extend(page_results)
                logger.info(f"Page {page}: {len(page_results)} items collected")

                # 마지막 페이지가 아니면 딜레이
                if page < max_pages:
                    time.sleep(random.uniform(1, 3))

            except Exception as e:
                # 통합 에러 처리
                should_break = self._handle_crawler_exception(
                    exception=e,
                    source=source,
                    url=url,
                    page=page,
                    page_num=page_num,
                    should_break=True
                )
                if should_break:
                    break
                else:
                    continue

        return all_results
    def crawl_volunteer(self, page_num: str = "253", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """봉사 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, "봉사", "volunteer", max_pages)
            logger.info(f"Volunteer crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling volunteer: {e}")
            return []
    def crawl_job(self, page_num: str = "248", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """취업 크롤링 (다중 페이지 지원) - 취업/채용 공지"""
        try:
            result = self._crawl_generic(page_num, "취업", "job", max_pages)
            logger.info(f"Job crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling job: {e}")
            return []
    def crawl_scholarship(self, page_num: str = "249", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """장학금 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, None, "scholarship", max_pages)  # category는 동적
            logger.info(f"Scholarship crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling scholarship: {e}")
            return []
    def crawl_general_events(self, page_num: str = "2611", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """일반행사/채용 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, None, "general_events", max_pages)
            logger.info(f"General events crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling general events: {e}")
            return []
    def crawl_educational_test(self, page_num: str = "252", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """교육시험 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, "교육시험", "educational_test", max_pages)
            logger.info(f"Educational test crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling educational test: {e}")
            return []

    def crawl_tuition_payment(self, page_num: str = "250", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """등록금납부 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, "등록금납부", "tuition_payment", max_pages)
            logger.info(f"Tuition payment crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling tuition payment: {e}")
            return []

    def crawl_academic_credit(self, page_num: str = "247", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """학점 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, "학점", "academic_credit", max_pages)
            logger.info(f"Academic credit crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling academic credit: {e}")
            return []

    def crawl_degree(self, page_num: str = "246", max_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """학위 크롤링 (다중 페이지 지원)"""
        try:
            result = self._crawl_generic(page_num, "학위", "degree", max_pages)
            logger.info(f"Degree crawling completed: {len(result)} items")
            return result
        except Exception as e:
            logger.error(f"Error crawling degree: {e}")
            return []

    def crawl_all(self) -> Dict[str, List[Dict[str, Any]]]:
        """모든 카테고리 크롤링"""
        logger.info("Starting comprehensive college crawling...")
        results = {}
        # 각 카테고리별로 크롤링 (간격을 두어 서버 부하 방지)
        try:
            results["volunteer"] = self.crawl_volunteer()
            time.sleep(random.uniform(1, 3))  # 1-3초 대기
            results["job"] = self.crawl_job()
            time.sleep(random.uniform(1, 3))
            results["scholarship"] = self.crawl_scholarship()
            time.sleep(random.uniform(1, 3))
            results["general_events"] = self.crawl_general_events()
            time.sleep(random.uniform(1, 3))
            results["educational_test"] = self.crawl_educational_test()
            time.sleep(random.uniform(1, 3))
            results["tuition_payment"] = self.crawl_tuition_payment()
            time.sleep(random.uniform(1, 3))
            results["academic_credit"] = self.crawl_academic_credit()
            time.sleep(random.uniform(1, 3))
            results["degree"] = self.crawl_degree()
        except Exception as e:
            logger.error(f"Error in comprehensive crawling: {e}")
        total_items = sum(len(items) for items in results.values())
        logger.info(f"Comprehensive crawling completed: {total_items} total items")
        return results
# 전역 인스턴스
college_crawler = CollegeCrawler()
def get_college_crawler() -> CollegeCrawler:
    """크롤러 인스턴스 반환"""
    return college_crawler
