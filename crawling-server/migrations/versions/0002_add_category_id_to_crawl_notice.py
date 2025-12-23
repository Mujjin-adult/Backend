"""
Alembic migration: Add category_id to crawl_notice table
"""

from alembic import op
import sqlalchemy as sa

revision = "0002_add_category_id"
down_revision = "0001_init"
branch_labels = None
depends_on = None


def upgrade():
    # Add category_id column to crawl_notice table
    op.add_column(
        "crawl_notice",
        sa.Column("category_id", sa.Integer(), nullable=True)
    )
    
    # Add foreign key constraint
    op.create_foreign_key(
        "fk_crawl_notice_category_id",
        "crawl_notice",
        "detail_category",
        ["category_id"],
        ["id"]
    )
    
    # Add index for performance
    op.create_index(
        "ix_crawl_notice_category_id",
        "crawl_notice",
        ["category_id"]
    )


def downgrade():
    # Drop index
    op.drop_index("ix_crawl_notice_category_id", table_name="crawl_notice")
    
    # Drop foreign key constraint
    op.drop_constraint("fk_crawl_notice_category_id", "crawl_notice", type_="foreignkey")
    
    # Drop column
    op.drop_column("crawl_notice", "category_id")
