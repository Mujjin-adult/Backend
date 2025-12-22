"""
Unit tests for college_crawlers.py
Tests pagination, parsing, and error handling
"""
import pytest
from bs4 import BeautifulSoup
from app.college_crawlers import CollegeCrawler, clean_category


class TestCleanCategory:
    """Tests for clean_category function"""

    def test_clean_category_with_brackets(self):
        """카테고리 텍스트에서 대괄호 제거"""
        assert clean_category("[봉사]") == "봉사"
        assert clean_category("[장학금]") == "장학금"

    def test_clean_category_without_brackets(self):
        """대괄호가 없는 경우 그대로 반환"""
        assert clean_category("봉사") == "봉사"

    def test_clean_category_empty(self):
        """빈 문자열은 그대로 반환"""
        assert clean_category("") == ""

    def test_clean_category_none(self):
        """None은 그대로 반환"""
        assert clean_category(None) is None


class TestCollegeCrawlerInit:
    """Tests for CollegeCrawler initialization"""

    def test_default_initialization(self):
        """기본 초기화 - max_pages=5"""
        crawler = CollegeCrawler()
        assert crawler.max_pages == 5
        assert crawler.base_url == "https://www.inu.ac.kr"

    def test_custom_max_pages(self):
        """커스텀 max_pages 설정"""
        crawler = CollegeCrawler(max_pages=10)
        assert crawler.max_pages == 10

    def test_session_headers(self):
        """세션 헤더 설정 확인"""
        crawler = CollegeCrawler()
        assert "User-Agent" in crawler.session.headers
        assert "Mozilla" in crawler.session.headers["User-Agent"]


class TestParseRow:
    """Tests for _parse_row method"""

    def test_parse_row_success(self, crawler):
        """정상적인 행 파싱"""
        html = """
        <tr>
            <td class="td-category">[봉사]</td>
            <td class="td-subject">
                <a href="/bbs/inu/253/artclView.do?artclSeq=123">봉사활동 안내</a>
            </td>
            <td class="td-write">학생지원팀</td>
            <td class="td-date">2025.11.01</td>
            <td class="td-access">150</td>
        </tr>
        """
        soup = BeautifulSoup(html, "html.parser")
        row = soup.find("tr")

        result = crawler._parse_row(row, None, "volunteer")

        assert result is not None
        assert result["title"] == "봉사활동 안내"
        assert result["writer"] == "학생지원팀"
        assert result["date"] == "2025.11.01"
        assert result["hits"] == "150"
        assert result["category"] == "봉사"  # 대괄호 제거됨
        assert result["source"] == "volunteer"
        assert result["url"] == "https://www.inu.ac.kr/bbs/inu/253/artclView.do?artclSeq=123"

    def test_parse_row_no_category_use_default(self, crawler):
        """카테고리가 없으면 기본값 사용"""
        html = """
        <tr>
            <td class="td-subject">
                <a href="/test.do">테스트</a>
            </td>
            <td class="td-write">작성자</td>
            <td class="td-date">2025.11.01</td>
            <td class="td-access">100</td>
        </tr>
        """
        soup = BeautifulSoup(html, "html.parser")
        row = soup.find("tr")

        result = crawler._parse_row(row, "기본카테고리", "test")

        assert result["category"] == "기본카테고리"

    def test_parse_row_no_link(self, crawler):
        """링크가 없는 행은 None 반환"""
        html = "<tr><td>링크 없음</td></tr>"
        soup = BeautifulSoup(html, "html.parser")
        row = soup.find("tr")

        result = crawler._parse_row(row, None, "test")

        assert result is None

    def test_parse_row_missing_fields(self, crawler):
        """일부 필드가 없어도 N/A로 처리"""
        html = """
        <tr>
            <td><a href="/test.do">제목만 있음</a></td>
        </tr>
        """
        soup = BeautifulSoup(html, "html.parser")
        row = soup.find("tr")

        result = crawler._parse_row(row, None, "test")

        assert result is not None
        assert result["writer"] == "N/A"
        assert result["date"] == "N/A"
        assert result["hits"] == "N/A"

    def test_parse_row_external_url(self, crawler):
        """외부 URL은 그대로 사용"""
        html = """
        <tr>
            <td><a href="https://external.com/notice">외부 공지</a></td>
            <td class="td-write">외부</td>
            <td class="td-date">2025.11.01</td>
            <td class="td-access">50</td>
        </tr>
        """
        soup = BeautifulSoup(html, "html.parser")
        row = soup.find("tr")

        result = crawler._parse_row(row, None, "test")

        assert result["url"] == "https://external.com/notice"


class TestCrawlGeneric:
    """Tests for _crawl_generic method"""

    def test_crawl_generic_single_page(self, crawler, mocker, mock_response_success):
        """단일 페이지 크롤링"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.return_value = mock_response_success

        results = crawler._crawl_generic("253", "봉사", "volunteer", max_pages=1)

        assert len(results) == 1
        assert results[0]["title"] == "테스트 공지"
        assert mock_post.call_count == 1

    def test_crawl_generic_multi_page(self, crawler, mocker, mock_response_success):
        """다중 페이지 크롤링"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.return_value = mock_response_success
        mock_sleep = mocker.patch("app.college_crawlers.time.sleep")

        results = crawler._crawl_generic("253", "봉사", "volunteer", max_pages=3)

        assert len(results) == 3
        assert mock_post.call_count == 3
        # 마지막 페이지 후에는 sleep 안 함
        assert mock_sleep.call_count == 2

    def test_crawl_generic_empty_page_stops(self, crawler, mocker, mock_response_empty):
        """빈 페이지를 만나면 조기 종료"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.return_value = mock_response_empty

        results = crawler._crawl_generic("253", "봉사", "volunteer", max_pages=5)

        assert len(results) == 0
        assert mock_post.call_count == 1  # 첫 페이지에서 멈춤

    def test_crawl_generic_error_handling(self, crawler, mocker):
        """에러 발생 시 빈 리스트 반환하고 중단"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.side_effect = Exception("Network error")

        results = crawler._crawl_generic("253", "봉사", "volunteer", max_pages=3)

        assert results == []
        assert mock_post.call_count == 1  # 첫 에러에서 중단


class TestVolunteerCrawler:
    """Tests for crawl_volunteer method"""

    def test_crawl_volunteer_success(self, crawler, mocker):
        """봉사 크롤링 성공"""
        mock_generic = mocker.patch.object(crawler, "_crawl_generic")
        mock_generic.return_value = [{"title": "봉사1"}, {"title": "봉사2"}]

        results = crawler.crawl_volunteer(max_pages=2)

        assert len(results) == 2
        mock_generic.assert_called_once_with("253", "봉사", "volunteer", 2)

    def test_crawl_volunteer_error(self, crawler, mocker):
        """봉사 크롤링 에러 처리"""
        mock_generic = mocker.patch.object(crawler, "_crawl_generic")
        mock_generic.side_effect = Exception("Error")

        results = crawler.crawl_volunteer()

        assert results == []


class TestJobCrawler:
    """Tests for crawl_job method"""

    def test_crawl_job_multi_page(self, crawler, mocker, mock_response_success):
        """취업 크롤링 다중 페이지"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.return_value = mock_response_success
        mock_sleep = mocker.patch("app.college_crawlers.time.sleep")

        results = crawler.crawl_job(max_pages=2)

        assert len(results) == 2
        assert mock_post.call_count == 2
        assert mock_sleep.call_count == 1  # 페이지 간 딜레이

    def test_crawl_job_uses_different_url(self, crawler, mocker, mock_response_success):
        """취업은 다른 URL 사용"""
        mock_post = mocker.patch.object(crawler.session, "post")
        mock_post.return_value = mock_response_success

        crawler.crawl_job(max_pages=1)

        call_args = mock_post.call_args[0]
        assert "employment/inu/employmentlist.do" in call_args[0]


class TestScholarshipCrawler:
    """Tests for crawl_scholarship method"""

    def test_crawl_scholarship_default_page_num(self, crawler, mocker):
        """장학금 크롤링 기본 페이지 번호"""
        mock_generic = mocker.patch.object(crawler, "_crawl_generic")
        mock_generic.return_value = []

        crawler.crawl_scholarship()

        mock_generic.assert_called_once_with("249", None, "scholarship", None)

    def test_crawl_scholarship_custom_page_num(self, crawler, mocker):
        """장학금 크롤링 커스텀 페이지 번호"""
        mock_generic = mocker.patch.object(crawler, "_crawl_generic")
        mock_generic.return_value = []

        crawler.crawl_scholarship(page_num="999", max_pages=3)

        mock_generic.assert_called_once_with("999", None, "scholarship", 3)


@pytest.mark.integration
class TestCrawlAll:
    """Tests for crawl_all method"""

    def test_crawl_all_calls_all_methods(self, crawler, mocker):
        """crawl_all이 모든 카테고리 크롤링 호출"""
        # Mock all individual crawlers
        mock_volunteer = mocker.patch.object(crawler, "crawl_volunteer", return_value=[{"id": 1}])
        mock_job = mocker.patch.object(crawler, "crawl_job", return_value=[{"id": 2}])
        mock_scholarship = mocker.patch.object(crawler, "crawl_scholarship", return_value=[{"id": 3}])
        mock_general = mocker.patch.object(crawler, "crawl_general_events", return_value=[{"id": 4}])
        mock_edu = mocker.patch.object(crawler, "crawl_educational_test", return_value=[{"id": 5}])
        mock_tuition = mocker.patch.object(crawler, "crawl_tuition_payment", return_value=[{"id": 6}])
        mock_credit = mocker.patch.object(crawler, "crawl_academic_credit", return_value=[{"id": 7}])
        mock_degree = mocker.patch.object(crawler, "crawl_degree", return_value=[{"id": 8}])
        mock_sleep = mocker.patch("app.college_crawlers.time.sleep")

        results = crawler.crawl_all()

        # 모든 메서드가 호출되었는지 확인
        assert mock_volunteer.called
        assert mock_job.called
        assert mock_scholarship.called
        assert mock_general.called
        assert mock_edu.called
        assert mock_tuition.called
        assert mock_credit.called
        assert mock_degree.called

        # 결과 확인
        assert "volunteer" in results
        assert "job" in results
        assert "scholarship" in results
        assert len(results) == 8

        # 딜레이가 각 크롤링 사이에 있는지 확인
        assert mock_sleep.call_count == 7  # 8개 크롤링, 7번의 딜레이
