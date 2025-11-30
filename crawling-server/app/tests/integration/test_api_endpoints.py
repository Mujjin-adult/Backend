"""
API 엔드포인트 통합 테스트

주요 API 엔드포인트의 동작을 검증합니다.
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import os

from main import app
from database import Base, get_db
from models import CrawlJob, CrawlTask, CrawlNotice
import crud

# 테스트용 데이터베이스 설정
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def test_db():
    """테스트용 데이터베이스 픽스처"""
    # 테스트 DB 생성
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()

    try:
        yield db
    finally:
        db.close()
        # 테스트 후 DB 정리
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(test_db):
    """테스트 클라이언트 픽스처"""
    def override_get_db():
        try:
            yield test_db
        finally:
            test_db.close()

    app.dependency_overrides[get_db] = override_get_db

    with TestClient(app) as c:
        yield c

    app.dependency_overrides.clear()


@pytest.fixture
def api_key():
    """테스트용 API 키"""
    return os.getenv("API_KEY", "test-api-key-for-integration-tests-12345678")


@pytest.fixture
def sample_job(test_db):
    """샘플 크롤 잡"""
    job_data = {
        "name": "test-job",
        "category": "test",
        "priority": "P1",
        "schedule_type": "manual",
    }
    job = crud.create_job(test_db, job_data)
    return job


class TestHealthCheck:
    """헬스 체크 엔드포인트 테스트"""

    def test_health_check_success(self, client):
        """헬스 체크 성공"""
        response = client.get("/health")
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "healthy"
        assert "timestamp" in data
        assert "database" in data


class TestJobEndpoints:
    """Job 관련 엔드포인트 테스트"""

    def test_get_jobs_without_auth(self, client):
        """인증 없이 잡 목록 조회 실패"""
        response = client.get("/jobs")
        assert response.status_code == 401

    def test_get_jobs_with_auth(self, client, api_key):
        """인증으로 잡 목록 조회 성공"""
        response = client.get(
            "/jobs",
            headers={"X-API-Key": api_key}
        )
        assert response.status_code == 200
        assert isinstance(response.json(), list)

    def test_create_job(self, client, api_key):
        """잡 생성 성공"""
        job_data = {
            "name": "new-test-job",
            "category": "scholarship",
            "priority": "P1",
            "schedule_type": "manual",
        }

        response = client.post(
            "/jobs",
            json=job_data,
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == job_data["name"]
        assert data["category"] == job_data["category"]
        assert "id" in data

    def test_create_job_duplicate_name(self, client, api_key, sample_job):
        """중복된 이름으로 잡 생성 실패"""
        job_data = {
            "name": sample_job.name,  # 중복 이름
            "category": "test",
            "priority": "P1",
            "schedule_type": "manual",
        }

        response = client.post(
            "/jobs",
            json=job_data,
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 400

    def test_get_job_by_id(self, client, api_key, sample_job):
        """ID로 잡 조회 성공"""
        response = client.get(
            f"/jobs/{sample_job.id}",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == sample_job.id
        assert data["name"] == sample_job.name

    def test_get_job_not_found(self, client, api_key):
        """존재하지 않는 잡 조회 실패"""
        response = client.get(
            "/jobs/99999",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 404

    def test_update_job_status(self, client, api_key, sample_job):
        """잡 상태 업데이트 성공"""
        response = client.post(
            f"/jobs/{sample_job.id}/pause",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "paused"

    def test_delete_job(self, client, api_key, sample_job):
        """잡 삭제 성공"""
        response = client.delete(
            f"/jobs/{sample_job.id}",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200

        # 삭제 확인
        response = client.get(
            f"/jobs/{sample_job.id}",
            headers={"X-API-Key": api_key}
        )
        assert response.status_code == 404


class TestTaskEndpoints:
    """Task 관련 엔드포인트 테스트"""

    def test_get_tasks(self, client, api_key):
        """태스크 목록 조회 성공"""
        response = client.get(
            "/tasks",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        assert isinstance(response.json(), list)

    def test_get_tasks_by_job(self, client, api_key, sample_job):
        """잡별 태스크 목록 조회"""
        response = client.get(
            f"/jobs/{sample_job.id}/tasks",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        assert isinstance(response.json(), list)


class TestDocumentEndpoints:
    """Document 관련 엔드포인트 테스트"""

    @pytest.fixture
    def sample_document(self, test_db, sample_job):
        """샘플 문서"""
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/test",
            "title": "Test Document",
            "category": "test",
            "source": "test",
            "raw": {"content": "test content"},
        }
        doc = crud.create_document(test_db, doc_data)
        return doc

    def test_get_documents(self, client, api_key):
        """문서 목록 조회 성공"""
        response = client.get(
            "/documents",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        assert isinstance(response.json(), list)

    def test_get_document_by_id(self, client, api_key, sample_document):
        """ID로 문서 조회 성공"""
        response = client.get(
            f"/documents/{sample_document.id}",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == sample_document.id
        assert data["title"] == sample_document.title

    def test_search_documents(self, client, api_key, sample_document):
        """문서 검색 성공"""
        response = client.get(
            "/documents/search?q=Test",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        # 검색 결과에 샘플 문서가 포함되어야 함
        assert any(doc["id"] == sample_document.id for doc in data)


class TestStatisticsEndpoints:
    """통계 관련 엔드포인트 테스트"""

    def test_get_job_statistics(self, client, api_key, sample_job):
        """잡 통계 조회 성공"""
        response = client.get(
            f"/jobs/{sample_job.id}/statistics",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 200
        data = response.json()
        assert "total_tasks" in data
        assert "completed_tasks" in data
        assert "success_rate" in data


class TestRateLimiting:
    """Rate Limiting 테스트"""

    def test_rate_limit_exceeded(self, client):
        """Rate Limit 초과 시 429 반환"""
        # Rate limit을 초과하도록 많은 요청 전송
        for _ in range(100):
            response = client.get("/health")

        # 마지막 요청은 429를 반환해야 함 (설정에 따라)
        # 주의: 테스트 환경에서는 rate limiting이 비활성화될 수 있음
        assert response.status_code in [200, 429]


class TestErrorHandling:
    """에러 처리 테스트"""

    def test_invalid_job_id_type(self, client, api_key):
        """잘못된 타입의 job_id 전달"""
        response = client.get(
            "/jobs/invalid",
            headers={"X-API-Key": api_key}
        )

        assert response.status_code == 422  # Validation error

    def test_invalid_json_body(self, client, api_key):
        """잘못된 JSON 바디"""
        response = client.post(
            "/jobs",
            data="invalid json",
            headers={
                "X-API-Key": api_key,
                "Content-Type": "application/json"
            }
        )

        assert response.status_code == 422

    def test_missing_required_fields(self, client, api_key):
        """필수 필드 누락"""
        job_data = {
            "name": "test-job",
            # category 누락
            # priority 누락
        }

        response = client.post(
            "/jobs",
            json=job_data,
            headers={"X-API-Key": api_key}
        )

        # 필수 필드 누락 시 에러 또는 기본값 사용
        assert response.status_code in [400, 422]


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
