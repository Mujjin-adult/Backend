"""
SQLAlchemy 모델 정의
Spring Boot의 JPA 엔티티와 동일한 구조
"""
from sqlalchemy import Column, Integer, String, Text, DateTime, Boolean, ForeignKey, Index
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base


class Category(Base):
    """카테고리 모델 (Spring Boot의 Category 엔티티와 동일)"""
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, autoincrement=True)
    code = Column(String(50), unique=True, nullable=False, index=True)
    name = Column(String(100), nullable=False)
    type = Column(String(20), nullable=False)  # CategoryType enum
    url = Column(String(255))
    is_active = Column(Boolean, default=True, nullable=False)
    description = Column(String(500))

    # BaseEntity 필드
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # Relationship
    notices = relationship("Notice", back_populates="category")


class Notice(Base):
    """공지사항 모델 (Spring Boot의 Notice 엔티티와 동일)"""
    __tablename__ = "notices"
    __table_args__ = (
        Index('idx_category_id', 'category_id'),
        Index('idx_published_at', 'published_at'),
        Index('idx_external_id', 'external_id'),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(500), nullable=False)
    content = Column(Text)
    url = Column(String(1000), nullable=False)
    external_id = Column(String(100), unique=True)
    category_id = Column(Integer, ForeignKey("categories.id"), nullable=False)
    author = Column(String(100))
    published_at = Column(DateTime(timezone=True), nullable=False)
    view_count = Column(Integer)
    is_important = Column(Boolean, default=False, nullable=False)
    is_pinned = Column(Boolean, default=False, nullable=False)
    attachments = Column(Text)

    # BaseEntity 필드
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # Relationship
    category = relationship("Category", back_populates="notices")
