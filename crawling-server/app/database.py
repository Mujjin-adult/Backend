"""
PostgreSQL 데이터베이스 연결 설정
"""
import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

load_dotenv()

# PostgreSQL 연결 URL
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:postgres@host.docker.internal:5432/incheon_notice"
)

# SQLAlchemy 엔진 생성
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,  # 연결 유효성 체크
    pool_size=10,        # 커넥션 풀 크기
    max_overflow=20,     # 최대 오버플로우
    echo=False           # SQL 로그 출력 (개발 시 True)
)

# 세션 팩토리
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base 클래스
Base = declarative_base()


def get_db():
    """데이터베이스 세션 생성"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """데이터베이스 초기화 (테이블 생성)"""
    # 이미 Spring Boot에서 테이블이 생성되어 있으므로 실행하지 않음
    # Base.metadata.create_all(bind=engine)
    pass
