# Fix: Add category_id to crawl_notice inserts

## Problem
The crawling server was inserting records into the `crawl_notice` table without the `category_id` field, even though this field exists in the Java backend entity and is needed for proper integration with the main server.

**Error in logs:**
```sql
INSERT INTO crawl_notice (job_id, url, title, writer, date, hits, category, source, content, extracted, raw, snapshot_version, fingerprint) 
VALUES (%(job_id)s, %(url)s, %(title)s, %(writer)s, %(date)s, %(hits)s, %(category)s, %(source)s, %(content)s, %(extracted)s, %(raw)s, %(snapshot_version)s, %(fingerprint)s)
```

Notice that `category_id` is missing from the INSERT statement.

## Root Cause
The Python `CrawlNotice` model was missing the `category_id` field and its relationship to the `DetailCategory` table, while the Java entity had this field defined as a Foreign Key.

## Solution

### 1. Updated the CrawlNotice Model (`models.py`)
Added the `category_id` field with:
- Foreign Key relationship to `detail_category.id`
- Index for performance
- Nullable constraint (allows backward compatibility)
- SQLAlchemy relationship to `DetailCategory`

```python
category_id = Column(Integer, ForeignKey("detail_category.id"), nullable=True, index=True)
detail_category = relationship("DetailCategory")
```

### 2. Created Database Migration (`0002_add_category_id_to_crawl_notice.py`)
- Adds `category_id` column to the `crawl_notice` table
- Creates foreign key constraint to `detail_category` table
- Adds index for query performance
- Includes downgrade function for rollback

### 3. Enhanced CRUD Operations (`crud.py`)
Added `get_or_create_detail_category()` function that:
- Looks up existing categories by name
- Creates new categories automatically if they don't exist
- Returns None for empty/null category names
- Handles errors gracefully with logging

### 4. Updated Task Processing (`tasks.py`)
Modified `college_crawl_task()` to:
- Look up or create the `DetailCategory` for each notice
- Populate `category_id` in the document data before insertion
- Apply this logic to both list and dictionary result processing paths

### 5. Added Comprehensive Tests (`test_category_id.py`)
Created unit tests to verify:
- Creating new detail categories
- Retrieving existing categories (no duplicates)
- Handling empty/null category names
- Creating documents with category_id
- Bulk creating documents with category_id
- Backward compatibility (documents without category_id)

## Changes Summary

| File | Changes |
|------|---------|
| `models.py` | Added `category_id` column and relationship |
| `crud.py` | Added `get_or_create_detail_category()` function |
| `tasks.py` | Populate `category_id` when creating documents |
| `migrations/0002_*.py` | Database migration to add column |
| `tests/unit/test_category_id.py` | Comprehensive test coverage |

## Benefits

1. **Database Consistency**: The `crawl_notice` table now properly references `detail_category`
2. **Main Server Integration**: Java backend can now query notices by `category_id`
3. **Automatic Category Management**: Categories are automatically created as notices are crawled
4. **Backward Compatibility**: Existing code continues to work (category_id is nullable)
5. **Performance**: Index on category_id improves query performance

## Migration Steps

To apply these changes:

1. Pull the latest code
2. Run the database migration:
   ```bash
   cd crawling-server
   alembic upgrade head
   ```
3. Restart the crawler services
4. Verify that new notices have `category_id` populated

## Verification

After deployment, verify by:

1. Checking the database schema:
   ```sql
   \d crawl_notice
   -- Should show category_id column with FK constraint
   ```

2. Checking inserted records:
   ```sql
   SELECT id, title, category, category_id FROM crawl_notice ORDER BY created_at DESC LIMIT 10;
   -- Should show category_id populated for new records
   ```

3. Checking detail_category table:
   ```sql
   SELECT * FROM detail_category;
   -- Should show automatically created categories
   ```

## Notes

- The `category` string field is still maintained for backward compatibility
- The `category_id` is nullable to allow historical data without breaking
- Categories are automatically synchronized when notices are crawled
- The relationship between CrawlNotice and DetailCategory is one-to-many
