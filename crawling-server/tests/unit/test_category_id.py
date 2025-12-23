"""
Test for category_id functionality
"""

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import json

from database import Base
from models import CrawlJob, CrawlNotice, DetailCategory
import crud

# Test database
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    """Test database session"""
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()

    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture
def sample_job(db_session):
    """Sample job fixture"""
    job_data = {
        "name": "test-category-job",
        "priority": "P1",
        "seed_type": "URL_LIST",
        "seed_payload": {"urls": ["https://example.com"]},
        "render_mode": "STATIC",
        "robots_policy": "OBEY",
    }
    return crud.create_job(db_session, job_data)


class TestCategoryId:
    """Test category_id functionality"""

    def test_get_or_create_detail_category_new(self, db_session):
        """Test creating a new detail category"""
        category_name = "장학금"
        
        # Should create a new category
        category = crud.get_or_create_detail_category(db_session, category_name)
        
        assert category is not None
        assert category.id is not None
        assert category.name == category_name
        assert category.is_active is True

    def test_get_or_create_detail_category_existing(self, db_session):
        """Test getting an existing detail category"""
        category_name = "취업"
        
        # Create first time
        category1 = crud.get_or_create_detail_category(db_session, category_name)
        first_id = category1.id
        
        # Get second time (should not create new)
        category2 = crud.get_or_create_detail_category(db_session, category_name)
        
        assert category2 is not None
        assert category2.id == first_id  # Same ID
        
        # Verify only one category exists
        all_categories = crud.get_all_detail_categories(db_session)
        matching = [c for c in all_categories if c.name == category_name]
        assert len(matching) == 1

    def test_get_or_create_detail_category_empty(self, db_session):
        """Test with empty category name"""
        category = crud.get_or_create_detail_category(db_session, "")
        assert category is None
        
        category = crud.get_or_create_detail_category(db_session, None)
        assert category is None

    def test_create_document_with_category_id(self, db_session, sample_job):
        """Test creating a document with category_id"""
        category_name = "봉사"
        
        # Create category first
        detail_category = crud.get_or_create_detail_category(db_session, category_name)
        
        # Create document with category_id
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/test-doc",
            "title": "Test Document with Category ID",
            "category": category_name,
            "category_id": detail_category.id,
            "source": "volunteer",
            "raw": json.dumps({"content": "test"}),
            "fingerprint": "test-fingerprint-with-category",
        }
        
        doc = crud.create_document(db_session, doc_data)
        
        assert doc.id is not None
        assert doc.category == category_name
        assert doc.category_id == detail_category.id
        
        # Verify relationship works
        assert doc.detail_category is not None
        assert doc.detail_category.name == category_name

    def test_bulk_create_documents_with_category_id(self, db_session, sample_job):
        """Test bulk creating documents with category_id"""
        categories = ["봉사", "취업", "장학금"]
        
        # Create categories
        category_map = {}
        for cat_name in categories:
            cat = crud.get_or_create_detail_category(db_session, cat_name)
            category_map[cat_name] = cat.id
        
        # Create documents
        docs_data = []
        for i, cat_name in enumerate(categories):
            docs_data.append({
                "job_id": sample_job.id,
                "url": f"https://example.com/doc-{i}",
                "title": f"Document {i}",
                "category": cat_name,
                "category_id": category_map[cat_name],
                "source": "test",
                "raw": json.dumps({"content": f"content {i}"}),
                "fingerprint": f"fingerprint-{i}",
            })
        
        count = crud.bulk_create_documents(db_session, docs_data)
        assert count == 3
        
        # Verify all documents have correct category_id
        docs = crud.get_documents(db_session)
        assert len(docs) == 3
        
        for doc in docs:
            assert doc.category_id is not None
            assert doc.category in categories

    def test_document_without_category_id(self, db_session, sample_job):
        """Test creating a document without category_id (backward compatibility)"""
        doc_data = {
            "job_id": sample_job.id,
            "url": "https://example.com/no-category",
            "title": "Document without category ID",
            "category": None,
            "category_id": None,
            "source": "test",
            "raw": json.dumps({}),
            "fingerprint": "no-category-fingerprint",
        }
        
        doc = crud.create_document(db_session, doc_data)
        
        assert doc.id is not None
        assert doc.category is None
        assert doc.category_id is None


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
