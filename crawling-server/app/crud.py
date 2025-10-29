"""
데이터베이스 CRUD 작업
"""
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from typing import Dict, Any, Optional
import logging
from datetime import datetime

from models import Notice, Category

logger = logging.getLogger(__name__)


def get_category_by_code(db: Session, code: str) -> Optional[Category]:
    """카테고리 코드로 카테고리 조회"""
    return db.query(Category).filter(Category.code == code).first()


def create_or_update_notice(db: Session, notice_data: Dict[str, Any]) -> tuple[Notice, bool]:
    """
    공지사항 생성 또는 업데이트

    Args:
        db: 데이터베이스 세션
        notice_data: 공지사항 데이터

    Returns:
        (Notice 객체, 생성 여부)
        생성된 경우 True, 업데이트된 경우 False
    """
    external_id = notice_data.get("external_id")

    if not external_id:
        raise ValueError("external_id는 필수입니다")

    # 기존 공지사항 확인
    existing_notice = db.query(Notice).filter(Notice.external_id == external_id).first()

    # 카테고리 조회
    category_code = notice_data.get("category_code")
    category = get_category_by_code(db, category_code)

    if not category:
        raise ValueError(f"카테고리를 찾을 수 없습니다: {category_code}")

    if existing_notice:
        # 업데이트
        existing_notice.title = notice_data.get("title", existing_notice.title)
        existing_notice.content = notice_data.get("content", existing_notice.content)
        existing_notice.author = notice_data.get("author", existing_notice.author)
        existing_notice.view_count = notice_data.get("view_count", existing_notice.view_count)
        existing_notice.is_important = notice_data.get("is_important", existing_notice.is_important)
        existing_notice.attachments = notice_data.get("attachments", existing_notice.attachments)
        existing_notice.updated_at = datetime.now()

        db.commit()
        db.refresh(existing_notice)

        logger.info(f"공지사항 업데이트: {existing_notice.title} (ID: {existing_notice.id})")
        return existing_notice, False

    else:
        # 새로 생성
        new_notice = Notice(
            title=notice_data.get("title"),
            content=notice_data.get("content", ""),
            url=notice_data.get("url"),
            external_id=external_id,
            category_id=category.id,
            author=notice_data.get("author"),
            published_at=notice_data.get("published_at"),
            view_count=notice_data.get("view_count", 0),
            is_important=notice_data.get("is_important", False),
            is_pinned=False,
            attachments=notice_data.get("attachments"),
        )

        db.add(new_notice)
        db.commit()
        db.refresh(new_notice)

        logger.info(f"새 공지사항 생성: {new_notice.title} (ID: {new_notice.id})")
        return new_notice, True


def save_notice(db: Session, notice_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    공지사항 저장 (생성 또는 업데이트)

    Returns:
        {"notice_id": int, "created": bool, "title": str}
    """
    try:
        notice, created = create_or_update_notice(db, notice_data)
        return {
            "notice_id": notice.id,
            "created": created,
            "title": notice.title,
            "external_id": notice.external_id,
        }
    except IntegrityError as e:
        db.rollback()
        logger.error(f"데이터베이스 무결성 오류: {str(e)}")
        raise
    except Exception as e:
        db.rollback()
        logger.error(f"공지사항 저장 실패: {str(e)}")
        raise


def get_notice_by_external_id(db: Session, external_id: str) -> Optional[Notice]:
    """외부 ID로 공지사항 조회"""
    return db.query(Notice).filter(Notice.external_id == external_id).first()


def get_all_notices(db: Session, limit: int = 100, offset: int = 0) -> list[Notice]:
    """모든 공지사항 조회 (페이지네이션)"""
    return db.query(Notice).order_by(Notice.published_at.desc()).offset(offset).limit(limit).all()


def count_notices(db: Session) -> int:
    """공지사항 총 개수"""
    return db.query(Notice).count()
