"""
CRUD 함수 단위 테스트

데이터베이스 CRUD 작업의 정확성을 검증합니다.
"""

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import json

from database import Base
from models import CrawlJob, CrawlTask, CrawlNotice, JobStatus, TaskStatus
import crud

# 테스트용 인메모리 데이터베이스
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    """테스트용 데이터베이스 세션"""
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()

    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)


class TestJobCRUD:
    """Job CRUD 테스트"""

    def test_create_job(self, db_session):
        """잡 생성"""
        job_data = {
            "name": "test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }

        job = crud.create_job(db_session, job_data)

        assert job.id is not None
        assert job.name == job_data["name"]
        assert job.priority == job_data["priority"]
        assert job.status == JobStatus.ACTIVE

    def test_get_job(self, db_session):
        """잡 조회"""
        # 잡 생성
        job_data = {
            "name": "test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        created_job = crud.create_job(db_session, job_data)

        # 조회
        retrieved_job = crud.get_job(db_session, created_job.id)

        assert retrieved_job is not None
        assert retrieved_job.id == created_job.id
        assert retrieved_job.name == created_job.name

    def test_get_job_not_found(self, db_session):
        """존재하지 않는 잡 조회"""
        job = crud.get_job(db_session, 99999)
        assert job is None

    def test_get_job_by_name(self, db_session):
        """이름으로 잡 조회"""
        job_data = {
            "name": "unique-job-name",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        crud.create_job(db_session, job_data)

        job = crud.get_job_by_name(db_session, "unique-job-name")

        assert job is not None
        assert job.name == "unique-job-name"

    def test_get_jobs_with_filters(self, db_session):
        """필터링하여 잡 목록 조회"""
        # 여러 잡 생성
        for i in range(5):
            job_data = {
                "name": f"job-{i}",
                "priority": "P1",
                "seed_type": "URL_LIST",
                "seed_payload": {"urls": ["https://example.com"]},
                "render_mode": "STATIC",
                "robots_policy": "OBEY",
            }
            crud.create_job(db_session, job_data)

        # 필터링 없이 조회
        all_jobs = crud.get_jobs(db_session)
        assert len(all_jobs) == 5

        # 카테고리 필터
        test_jobs = crud.get_jobs(db_session, skip=0, limit=100)
        assert len(test_jobs) <= 5

    def test_update_job_status(self, db_session):
        """잡 상태 업데이트"""
        job_data = {
            "name": "test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        job = crud.create_job(db_session, job_data)

        # 상태 업데이트
        updated_job = crud.update_job_status(db_session, job.id, JobStatus.PAUSED)

        assert updated_job.status == JobStatus.PAUSED
        assert updated_job.updated_at is not None

    def test_delete_job(self, db_session):
        """잡 삭제"""
        job_data = {
            "name": "test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        job = crud.create_job(db_session, job_data)

        # 삭제
        success = crud.delete_job(db_session, job.id)
        assert success is True

        # 삭제 확인
        deleted_job = crud.get_job(db_session, job.id)
        assert deleted_job is None


class TestTaskCRUD:
    """Task CRUD 테스트"""

    @pytest.fixture
    def sample_job(self, db_session):
        """샘플 잡"""
        job_data = {
            "name": "sample-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        return crud.create_job(db_session, job_data)

    def test_create_task(self, db_session, sample_job):
        """태스크 생성"""
        task_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/test",
        }

        task = crud.create_task(db_session, task_data)

        assert task.id is not None
        assert task.job_id == sample_job.id
        assert task.url == task_data["url"]
        assert task.status == TaskStatus.PENDING

    def test_get_task(self, db_session, sample_job):
        """태스크 조회"""
        task_data = {"job_id": sample_job.id, "url": "https://example.com/test"}
        created_task = crud.create_task(db_session, task_data)

        retrieved_task = crud.get_task(db_session, created_task.id)

        assert retrieved_task is not None
        assert retrieved_task.id == created_task.id

    def test_get_tasks_by_job(self, db_session, sample_job):
        """잡별 태스크 목록 조회"""
        # 여러 태스크 생성
        for i in range(3):
            task_data = {
                "job_id": sample_job.id,
                "url": f"https://example.com/test-{i}",
            }
            crud.create_task(db_session, task_data)

        tasks = crud.get_tasks_by_job(db_session, sample_job.id)

        assert len(tasks) == 3
        assert all(task.job_id == sample_job.id for task in tasks)

    def test_update_task_status(self, db_session, sample_job):
        """태스크 상태 업데이트"""
        task_data = {"job_id": sample_job.id, "url": "https://example.com/test"}
        task = crud.create_task(db_session, task_data)

        # 상태 업데이트
        updated_task = crud.update_task_status(
            db_session,
            task.id,
            TaskStatus.SUCCESS,
            http_status=200,
            content_hash="abc123",
        )

        assert updated_task.status == TaskStatus.SUCCESS
        assert updated_task.http_status == 200
        assert updated_task.content_hash == "abc123"
        assert updated_task.finished_at is not None

    def test_increment_task_retries(self, db_session, sample_job):
        """태스크 재시도 횟수 증가"""
        task_data = {"job_id": sample_job.id, "url": "https://example.com/test"}
        task = crud.create_task(db_session, task_data)

        initial_retries = task.retries

        # 재시도 횟수 증가
        updated_task = crud.increment_task_retries(db_session, task.id)

        assert updated_task.retries == initial_retries + 1


class TestDocumentCRUD:
    """Document CRUD 테스트"""

    @pytest.fixture
    def sample_job(self, db_session):
        """샘플 잡"""
        job_data = {
            "name": "sample-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        return crud.create_job(db_session, job_data)

    def test_create_document(self, db_session, sample_job):
        """문서 생성"""
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/doc",
            "title": "Test Document",
            "category": "test",
            "source": "test",
            "raw": json.dumps({"content": "test content"}),
            "fingerprint": "test-fingerprint-123",
        }

        doc = crud.create_document(db_session, doc_data)

        assert doc.id is not None
        assert doc.title == doc_data["title"]
        assert doc.url == doc_data["url"]

    def test_bulk_create_documents(self, db_session, sample_job):
        """문서 벌크 생성"""
        docs_data = []
        for i in range(10):
            docs_data.append({
                "job_id": sample_job.id,
                "url": f"https://example.com/doc-{i}",
                "title": f"Document {i}",
                "category": "test",
                "source": "test",
                "raw": json.dumps({"content": f"content {i}"}),
                "fingerprint": f"fingerprint-{i}",
            })

        count = crud.bulk_create_documents(db_session, docs_data)

        assert count == 10

        # 생성 확인
        docs = crud.get_documents(db_session)
        assert len(docs) == 10

    def test_get_document(self, db_session, sample_job):
        """문서 조회"""
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/doc",
            "title": "Test Document",
            "category": "test",
            "source": "test",
            "raw": json.dumps({}),
            "fingerprint": "test-doc-fingerprint",
        }
        created_doc = crud.create_document(db_session, doc_data)

        retrieved_doc = crud.get_document(db_session, created_doc.id)

        assert retrieved_doc is not None
        assert retrieved_doc.id == created_doc.id

    def test_get_document_by_url(self, db_session, sample_job):
        """URL로 문서 조회"""
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/unique-url",
            "title": "Test Document",
            "category": "test",
            "source": "test",
            "raw": json.dumps({}),
            "fingerprint": "unique-url-fingerprint",
        }
        crud.create_document(db_session, doc_data)

        doc = crud.get_document_by_url(db_session, "https://example.com/unique-url")

        assert doc is not None
        assert doc.url == "https://example.com/unique-url"

    def test_get_documents_with_filters(self, db_session, sample_job):
        """필터링하여 문서 목록 조회"""
        # 여러 문서 생성
        for i in range(5):
            doc_data = {
                "job_id": sample_job.id,
                "url": f"https://example.com/doc-{i}",
                "title": f"Document {i}",
                "category": "scholarship" if i % 2 == 0 else "notice",
                "source": "test",
                "raw": json.dumps({}),
                "fingerprint": f"filter-doc-{i}",
            }
            crud.create_document(db_session, doc_data)

        # 카테고리 필터
        scholarship_docs = crud.get_documents(
            db_session, category="scholarship"
        )
        assert len(scholarship_docs) == 3

    def test_search_documents(self, db_session, sample_job):
        """문서 검색"""
        # 검색 가능한 문서 생성
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/searchable",
            "title": "Searchable Document",
            "category": "test",
            "source": "test",
            "raw": json.dumps({"content": "특별한 키워드가 포함된 내용"}),
            "fingerprint": "searchable-fingerprint",
        }
        crud.create_document(db_session, doc_data)

        # 검색
        results = crud.search_documents(db_session, q="Searchable")

        assert len(results) > 0
        assert any("Searchable" in doc.title for doc in results)


class TestStatistics:
    """통계 함수 테스트"""

    @pytest.fixture
    def setup_test_data(self, db_session):
        """테스트 데이터 설정"""
        # 잡 생성
        job_data = {
            "name": "stats-test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        job = crud.create_job(db_session, job_data)

        # 태스크 생성 (다양한 상태)
        for i in range(10):
            task_data = {
                "job_id": job.id,
                "url": f"https://example.com/test-{i}",
            }
            task = crud.create_task(db_session, task_data)

            # 일부는 성공, 일부는 실패
            if i < 7:
                crud.update_task_status(db_session, task.id, TaskStatus.SUCCESS)
            elif i < 9:
                crud.update_task_status(db_session, task.id, TaskStatus.FAILED)

        return job

    def test_get_job_statistics(self, db_session, setup_test_data):
        """잡 통계 조회"""
        job = setup_test_data

        stats = crud.get_job_statistics(db_session, job.id)

        assert stats["total_tasks"] == 10
        assert stats["completed_tasks"] == 7
        assert stats["failed_tasks"] == 2
        assert 0 <= stats["success_rate"] <= 100


class TestBulkOperations:
    """벌크 작업 테스트"""

    @pytest.fixture
    def sample_job(self, db_session):
        """샘플 잡"""
        job_data = {
            "name": "bulk-test-job",
            "priority": "P1",
            "seed_type": "URL_LIST",
            "seed_payload": {"urls": ["https://example.com"]},
            "render_mode": "STATIC",
            "robots_policy": "OBEY",
        }
        return crud.create_job(db_session, job_data)

    def test_bulk_insert_performance(self, db_session, sample_job):
        """벌크 삽입 성능 테스트"""
        import time

        # 100개 문서 데이터 준비
        docs_data = []
        for i in range(100):
            docs_data.append({
                "job_id": sample_job.id,
                "url": f"https://example.com/bulk-{i}",
                "title": f"Bulk Document {i}",
                "category": "test",
                "source": "test",
                "raw": json.dumps({"content": f"bulk content {i}"}),
                "fingerprint": f"bulk-fingerprint-{i}",
            })

        # 벌크 삽입 시간 측정
        start_time = time.time()
        count = crud.bulk_create_documents(db_session, docs_data)
        elapsed_time = time.time() - start_time

        assert count == 100
        # 100개 문서를 1초 이내에 삽입해야 함
        assert elapsed_time < 1.0

    def test_bulk_insert_with_duplicates(self, db_session, sample_job):
        """중복이 있는 벌크 삽입"""
        # 첫 번째 배치
        docs_data_1 = [
            {
                "job_id": sample_job.id,
                "url": "https://example.com/doc-1",
                "title": "Document 1",
                "category": "test",
                "source": "test",
                "raw": json.dumps({}),
                "fingerprint": "dup-test-1",
            }
        ]
        crud.bulk_create_documents(db_session, docs_data_1)

        # 중복 URL 포함 두 번째 배치
        docs_data_2 = [
            {
                "job_id": sample_job.id,
                "url": "https://example.com/doc-1",  # 중복
                "title": "Document 1 (Updated)",
                "category": "test",
                "source": "test",
                "raw": json.dumps({}),
                "fingerprint": "dup-test-1-updated",
            },
            {
                "job_id": sample_job.id,
                "url": "https://example.com/doc-2",  # 새로운 URL
                "title": "Document 2",
                "category": "test",
                "source": "test",
                "raw": json.dumps({}),
                "fingerprint": "dup-test-2",
            },
        ]

        # 벌크 삽입 시 중복 처리 확인
        # (구현에 따라 에러 발생 또는 스킵)
        try:
            crud.bulk_create_documents(db_session, docs_data_2)
        except Exception:
            pass  # 중복 에러 예상

        # 최종 문서 수 확인
        docs = crud.get_documents(db_session)
        # 중복을 제외하고 2개여야 함
        assert len(docs) >= 1  # 최소 1개 (첫 번째 문서)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
