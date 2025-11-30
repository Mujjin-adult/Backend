"""
Integration tests for API endpoints
Tests the full FastAPI application with database
"""
import pytest
from app.models import CrawlJob, CrawlNotice, JobStatus


@pytest.mark.integration
class TestHealthEndpoint:
    """Tests for health check endpoint"""

    def test_health_check(self, client):
        """헬스 체크 엔드포인트 테스트"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"


@pytest.mark.integration
class TestDashboardEndpoint:
    """Tests for dashboard endpoint"""

    def test_dashboard_accessible(self, client):
        """대시보드 페이지 접근 가능"""
        response = client.get("/dashboard")
        assert response.status_code == 200


@pytest.mark.integration
class TestCrawlerEndpoints:
    """Tests for crawler trigger endpoints"""

    def test_run_crawler_requires_api_key(self, client):
        """API 키 없이 크롤러 실행 시도 시 401 에러"""
        response = client.post("/run-crawler/volunteer")
        assert response.status_code == 401

    def test_run_crawler_with_api_key(self, client):
        """API 키와 함께 크롤러 실행"""
        headers = {"X-API-Key": "dev-api-key-12345"}
        response = client.post("/run-crawler/volunteer", headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert "task_id" in data


@pytest.mark.integration
class TestDatabaseOperations:
    """Tests for database operations"""

    def test_create_crawl_job(self, db_session):
        """크롤링 작업 생성 테스트"""
        from app.models import SeedType, RenderMode, RobotsPolicy

        job = CrawlJob(
            name="테스트 작업",
            priority="P1",
            seed_type=SeedType.URL_LIST,
            seed_payload={"urls": ["https://example.com"]},
            render_mode=RenderMode.STATIC,
            robots_policy=RobotsPolicy.OBEY,
            status=JobStatus.ACTIVE
        )
        db_session.add(job)
        db_session.commit()

        assert job.id is not None
        assert job.name == "테스트 작업"

    def test_create_crawl_notice(self, db_session):
        """크롤링 공지사항 생성 테스트"""
        from app.models import SeedType, RenderMode, RobotsPolicy
        import hashlib

        # 먼저 작업 생성
        job = CrawlJob(
            name="테스트 작업",
            priority="P1",
            seed_type=SeedType.URL_LIST,
            seed_payload={"urls": ["https://example.com"]},
            render_mode=RenderMode.STATIC,
            robots_policy=RobotsPolicy.OBEY
        )
        db_session.add(job)
        db_session.commit()

        # 공지사항 생성
        notice_url = "https://www.inu.ac.kr/test/123"
        fingerprint = hashlib.sha256(notice_url.encode()).hexdigest()

        notice = CrawlNotice(
            job_id=job.id,
            url=notice_url,
            title="테스트 공지",
            writer="작성자",
            date="2025.11.01",
            hits="100",
            category="봉사",
            source="volunteer",
            fingerprint=fingerprint
        )
        db_session.add(notice)
        db_session.commit()

        assert notice.id is not None
        assert notice.title == "테스트 공지"
        assert notice.job_id == job.id

    def test_duplicate_fingerprint_handling(self, db_session):
        """중복 fingerprint 처리 테스트"""
        from app.models import SeedType, RenderMode, RobotsPolicy
        import hashlib

        # 작업 생성
        job = CrawlJob(
            name="테스트 작업",
            priority="P1",
            seed_type=SeedType.URL_LIST,
            seed_payload={"urls": ["https://example.com"]},
            render_mode=RenderMode.STATIC,
            robots_policy=RobotsPolicy.OBEY
        )
        db_session.add(job)
        db_session.commit()

        # 첫 번째 공지사항
        url = "https://www.inu.ac.kr/test/123"
        fingerprint = hashlib.sha256(url.encode()).hexdigest()

        notice1 = CrawlNotice(
            job_id=job.id,
            url=url,
            title="첫 번째 공지",
            fingerprint=fingerprint
        )
        db_session.add(notice1)
        db_session.commit()

        # 중복 체크 쿼리
        existing = db_session.query(CrawlNotice).filter_by(fingerprint=fingerprint).first()
        assert existing is not None
        assert existing.title == "첫 번째 공지"
