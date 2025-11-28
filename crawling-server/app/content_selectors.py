"""
공지사항 본문 크롤링을 위한 CSS 선택자 매핑

인천대학교 공지사항 사이트별 본문 추출 CSS 선택자를 정의합니다.
"""

from typing import Dict, List

# 인천대 공지사항 사이트별 CSS 선택자 매핑
CONTENT_SELECTORS: Dict[str, Dict[str, any]] = {
    # 봉사 (volunteer)
    "volunteer": {
        "base_url_pattern": "/bbs/inu/253/",
        "selectors": [
            ".view-con",            # 우선순위 1 (인천대 실제 구조)
            ".board-view-content",  # 우선순위 2
            ".view-content",        # 우선순위 3
            ".board-content",       # 우선순위 4
            "article",              # 우선순위 5
            ".content",             # 우선순위 6
        ],
        "fallback": "main, #content, .main-content"
    },

    # 취업 (job)
    "job": {
        "base_url_pattern": "/employment/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".employment-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 장학금 (scholarship)
    "scholarship": {
        "base_url_pattern": "/bbs/inu/249/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 일반행사/채용 (general_events)
    "general_events": {
        "base_url_pattern": "/bbs/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 교육시험 (educational_test)
    "educational_test": {
        "base_url_pattern": "/bbs/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 등록금납부 (tuition_payment)
    "tuition_payment": {
        "base_url_pattern": "/bbs/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 학점 (academic_credit)
    "academic_credit": {
        "base_url_pattern": "/bbs/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },

    # 학위 (degree)
    "degree": {
        "base_url_pattern": "/bbs/inu/",
        "selectors": [
            ".view-con",
            ".board-view-content",
            ".view-content",
            ".board-content",
            "article",
            ".content",
        ],
        "fallback": "main, #content"
    },
}

# 기본 선택자 (카테고리를 알 수 없을 때 사용)
DEFAULT_SELECTORS = [
    ".view-con",            # 인천대 실제 구조
    ".board-view-content",
    ".view-content",
    ".board-content",
    "article",
    ".content",
    "main",
    "#content",
]

# 제거할 HTML 요소 (본문 추출 시 제외)
ELEMENTS_TO_REMOVE = [
    "script",      # JavaScript
    "style",       # CSS
    "iframe",      # 광고 프레임
    ".ad",         # 광고
    ".advertisement",
    ".social-share",  # 공유 버튼
    ".comment",    # 댓글
    ".footer",     # 푸터
    ".navigation", # 네비게이션
    ".breadcrumb", # 브레드크럼
]


def get_selectors_by_url(url: str) -> List[str]:
    """
    URL을 기반으로 적절한 CSS 선택자 목록을 반환

    Args:
        url: 공지사항 상세 페이지 URL

    Returns:
        CSS 선택자 리스트 (우선순위 순)
    """
    # URL 패턴 매칭
    for category, config in CONTENT_SELECTORS.items():
        if config["base_url_pattern"] in url:
            return config["selectors"]

    # 매칭되지 않으면 기본 선택자 반환
    return DEFAULT_SELECTORS


def get_selectors_by_category(category: str) -> List[str]:
    """
    카테고리를 기반으로 CSS 선택자 목록을 반환

    Args:
        category: 카테고리 (volunteer, job, scholarship 등)

    Returns:
        CSS 선택자 리스트 (우선순위 순)
    """
    config = CONTENT_SELECTORS.get(category.lower())
    if config:
        return config["selectors"]

    return DEFAULT_SELECTORS


def get_fallback_selectors(category: str = None) -> str:
    """
    폴백 선택자 반환 (모든 우선순위 선택자가 실패했을 때 사용)

    Args:
        category: 카테고리 (선택사항)

    Returns:
        폴백 CSS 선택자 문자열
    """
    if category and category.lower() in CONTENT_SELECTORS:
        return CONTENT_SELECTORS[category.lower()]["fallback"]

    return "main, #content, body"
