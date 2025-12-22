"""
Pytest configuration and fixtures for the test suite
"""
import pytest
import sys
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(project_root / "app"))


@pytest.fixture
def crawler():
    """
    Create a CollegeCrawler instance for testing
    """
    from app.college_crawlers import CollegeCrawler
    return CollegeCrawler(max_pages=2)  # 테스트에서는 2페이지만


@pytest.fixture
def sample_html_row():
    """
    Sample HTML row for parsing tests
    """
    return """
    <tr>
        <td class="td-category">봉사</td>
        <td class="td-subject">
            <a href="/bbs/inu/253/artclView.do?artclSeq=123">
                2025년 봉사활동 모집 안내
            </a>
        </td>
        <td class="td-write">학생지원팀</td>
        <td class="td-date">2025.11.01</td>
        <td class="td-access">150</td>
    </tr>
    """


@pytest.fixture
def sample_notice_data():
    """
    Sample notice data for testing
    """
    return {
        "title": "2025년 봉사활동 모집 안내",
        "writer": "학생지원팀",
        "date": "2025.11.01",
        "hits": "150",
        "category": "봉사",
        "source": "volunteer",
        "url": "https://www.inu.ac.kr/bbs/inu/253/artclView.do?artclSeq=123"
    }


@pytest.fixture
def mock_response_success(mocker):
    """
    Mock successful HTTP response
    """
    mock_response = mocker.Mock()
    mock_response.status_code = 200
    mock_response.text = """
    <html>
        <table class="board-table">
            <tbody>
                <tr>
                    <td class="td-category">봉사</td>
                    <td class="td-subject">
                        <a href="/bbs/inu/253/artclView.do?artclSeq=123">테스트 공지</a>
                    </td>
                    <td class="td-write">작성자</td>
                    <td class="td-date">2025.11.01</td>
                    <td class="td-access">100</td>
                </tr>
            </tbody>
        </table>
    </html>
    """
    return mock_response


@pytest.fixture
def mock_response_empty(mocker):
    """
    Mock empty HTTP response (no data)
    """
    mock_response = mocker.Mock()
    mock_response.status_code = 200
    mock_response.text = """
    <html>
        <table class="board-table">
            <tbody></tbody>
        </table>
    </html>
    """
    return mock_response
