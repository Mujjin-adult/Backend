"""
공지사항 본문 크롤링 유틸리티

crawl_notice 테이블의 URL에서 본문 내용을 추출하여 content 필드에 저장합니다.
"""

import requests
from bs4 import BeautifulSoup
import logging
import time
import random
from typing import Optional, Dict, Any
from urllib.parse import urlparse

from app.content_selectors import (
    get_selectors_by_url,
    get_selectors_by_category,
    get_fallback_selectors,
    ELEMENTS_TO_REMOVE
)

logger = logging.getLogger(__name__)


class ContentCrawler:
    """공지사항 본문 크롤러"""

    def __init__(self, max_retries: int = 3):
        """
        초기화

        Args:
            max_retries: 최대 재시도 횟수 (기본값: 3)
        """
        self.max_retries = max_retries
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                          "AppleWebKit/537.36 (KHTML, like Gecko) "
                          "Chrome/91.0.4472.124 Safari/537.36"
        })

    def crawl_content(
        self,
        url: str,
        category: Optional[str] = None,
        retry_count: int = 0
    ) -> Dict[str, Any]:
        """
        URL에서 본문 내용을 크롤링

        Args:
            url: 공지사항 상세 페이지 URL
            category: 카테고리 (volunteer, job, scholarship 등)
            retry_count: 현재 재시도 횟수

        Returns:
            Dict containing:
                - success: 성공 여부 (bool)
                - content: 추출된 본문 (str, 성공 시)
                - error: 에러 메시지 (str, 실패 시)
                - method: 사용된 방법 ("beautifulsoup" 또는 "playwright")
        """
        try:
            # 1차 시도: BeautifulSoup (빠름)
            logger.info(f"Attempting to crawl content from: {url}")
            result = self._crawl_with_beautifulsoup(url, category)

            if result["success"]:
                logger.info(f"Successfully crawled content from: {url}")
                return result

            # BeautifulSoup 실패 시, 재시도 로직
            if retry_count < self.max_retries:
                wait_time = (2 ** retry_count) + random.uniform(0.5, 1.5)
                logger.warning(
                    f"Failed to crawl content, retrying in {wait_time:.1f}s "
                    f"(attempt {retry_count + 1}/{self.max_retries}): {url}"
                )
                time.sleep(wait_time)
                return self.crawl_content(url, category, retry_count + 1)
            else:
                logger.error(f"Max retries exceeded for: {url}")
                return {
                    "success": False,
                    "error": f"Max retries exceeded: {result.get('error', 'Unknown error')}",
                    "url": url
                }

        except Exception as e:
            logger.error(f"Unexpected error crawling {url}: {type(e).__name__}: {str(e)}")
            return {
                "success": False,
                "error": f"Unexpected error: {str(e)}",
                "url": url
            }

    def _crawl_with_beautifulsoup(
        self,
        url: str,
        category: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        BeautifulSoup을 사용하여 정적 HTML에서 본문 추출

        Args:
            url: 공지사항 상세 페이지 URL
            category: 카테고리

        Returns:
            Dict containing success, content/error, method
        """
        try:
            # HTTP 요청
            response = self.session.get(url, timeout=30)
            response.raise_for_status()

            # HTML 파싱
            soup = BeautifulSoup(response.text, 'html.parser')

            # CSS 선택자로 본문 추출
            content = self._extract_content_from_soup(soup, url, category)

            if content:
                # HTML 정제
                cleaned_content = self._clean_html_content(content)

                if cleaned_content and len(cleaned_content.strip()) > 10:
                    return {
                        "success": True,
                        "content": cleaned_content,
                        "method": "beautifulsoup",
                        "url": url
                    }
                else:
                    return {
                        "success": False,
                        "error": "Extracted content is too short or empty",
                        "url": url
                    }
            else:
                return {
                    "success": False,
                    "error": "Could not find content with any selector",
                    "url": url
                }

        except requests.exceptions.Timeout:
            return {
                "success": False,
                "error": "Request timeout",
                "url": url
            }
        except requests.exceptions.HTTPError as e:
            return {
                "success": False,
                "error": f"HTTP error: {e.response.status_code}",
                "url": url
            }
        except requests.exceptions.RequestException as e:
            return {
                "success": False,
                "error": f"Request error: {str(e)}",
                "url": url
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"Parsing error: {str(e)}",
                "url": url
            }

    def _extract_content_from_soup(
        self,
        soup: BeautifulSoup,
        url: str,
        category: Optional[str] = None
    ) -> Optional[str]:
        """
        BeautifulSoup 객체에서 CSS 선택자를 사용하여 본문 추출

        Args:
            soup: BeautifulSoup 객체
            url: URL (선택자 결정용)
            category: 카테고리 (선택자 결정용)

        Returns:
            추출된 본문 HTML 문자열 (실패 시 None)
        """
        # URL 기반 선택자 우선
        selectors = get_selectors_by_url(url)

        # 카테고리 정보가 있으면 카테고리 기반 선택자도 시도
        if category:
            category_selectors = get_selectors_by_category(category)
            # 중복 제거하면서 병합
            selectors = list(dict.fromkeys(selectors + category_selectors))

        # 각 선택자를 순차적으로 시도
        for selector in selectors:
            element = soup.select_one(selector)
            if element:
                logger.debug(f"Found content with selector: {selector}")
                return str(element)

        # 모든 선택자 실패 시, 폴백 선택자 시도
        fallback = get_fallback_selectors(category)
        for selector in fallback.split(','):
            selector = selector.strip()
            element = soup.select_one(selector)
            if element:
                logger.debug(f"Found content with fallback selector: {selector}")
                return str(element)

        logger.warning(f"Could not find content with any selector for: {url}")
        return None

    def _clean_html_content(self, html_content: str) -> str:
        """
        HTML 본문 정제 (스크립트, 스타일, 광고 등 제거)

        Args:
            html_content: 원본 HTML 문자열

        Returns:
            정제된 HTML 문자열
        """
        try:
            soup = BeautifulSoup(html_content, 'html.parser')

            # 제거할 요소들 삭제
            for selector in ELEMENTS_TO_REMOVE:
                for element in soup.select(selector):
                    element.decompose()

            # 불필요한 속성 제거 (onclick, onerror 등 이벤트 핸들러)
            for tag in soup.find_all(True):
                # 이벤트 핸들러 속성 제거
                event_attrs = [attr for attr in tag.attrs if attr.startswith('on')]
                for attr in event_attrs:
                    del tag[attr]

            # 정제된 HTML 반환 (공백 정리)
            cleaned = str(soup).strip()

            # 텍스트만 추출하려면 이 줄을 사용
            # cleaned = soup.get_text(separator='\n', strip=True)

            return cleaned

        except Exception as e:
            logger.error(f"Error cleaning HTML content: {e}")
            return html_content  # 정제 실패 시 원본 반환


# 싱글톤 인스턴스
_crawler_instance = None


def get_content_crawler() -> ContentCrawler:
    """
    ContentCrawler 싱글톤 인스턴스 반환

    Returns:
        ContentCrawler 인스턴스
    """
    global _crawler_instance
    if _crawler_instance is None:
        _crawler_instance = ContentCrawler(max_retries=3)
    return _crawler_instance
