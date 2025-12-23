# Solution Overview: category_id Integration

## Before (Problem)
```
college_crawl_task()
    ↓
  crawl_volunteer() → returns items with "category": "봉사"
    ↓
  INSERT INTO crawl_notice:
    ✅ category = "봉사"
    ❌ category_id = NULL  ← Missing!
```

## After (Solution)
```
college_crawl_task()
    ↓
  crawl_volunteer() → returns items with "category": "봉사"
    ↓
  get_category_id_for_item()
    ↓
  get_or_create_detail_category("봉사")
    ├─ Query detail_category WHERE name = "봉사"
    └─ If not found: INSERT INTO detail_category (name="봉사")
    ↓
  Returns category_id = 1
    ↓
  INSERT INTO crawl_notice:
    ✅ category = "봉사"
    ✅ category_id = 1  ← Now populated!
```

## Database Schema
```
┌─────────────────────┐         ┌─────────────────────┐
│  detail_category    │         │   crawl_notice      │
├─────────────────────┤         ├─────────────────────┤
│ id (PK)             │◄────────│ id (PK)             │
│ name (unique)       │    FK   │ category (string)   │
│ description         │         │ category_id (FK)    │◄─ NEW!
│ is_active           │         │ url                 │
│ created_at          │         │ title               │
│ updated_at          │         │ ...                 │
└─────────────────────┘         └─────────────────────┘

Example data:
detail_category:               crawl_notice:
┌────┬──────────┐             ┌────┬───────────┬─────────────┬────────┐
│ id │   name   │             │ id │  category │ category_id │ title  │
├────┼──────────┤             ├────┼───────────┼─────────────┼────────┤
│  1 │   봉사    │             │ 10 │   봉사     │      1      │ 봉사... │
│  2 │   취업    │             │ 11 │   취업     │      2      │ 취업... │
│  3 │  장학금   │             │ 12 │  장학금    │      3      │ 장학... │
└────┴──────────┘             └────┴───────────┴─────────────┴────────┘
```

## Code Flow

### 1. Helper Function (tasks.py)
```python
def get_category_id_for_item(db, item):
    """Get or create category_id for a crawled item"""
    category_id = None
    category_name = item.get('category')
    if category_name:
        detail_category = get_or_create_detail_category(db, category_name)
        if detail_category:
            category_id = detail_category.id
    return category_id
```

### 2. CRUD Function (crud.py)
```python
def get_or_create_detail_category(db, category_name):
    """Look up or create a detail_category"""
    if not category_name:
        return None
    
    # Try to find existing
    category = get_detail_category_by_name(db, category_name)
    
    # Create if not found
    if not category:
        category = create_detail_category(db, name=category_name)
    
    return category
```

### 3. Usage in Task (tasks.py)
```python
for item in results:
    # Get or create category_id
    category_id = get_category_id_for_item(db, item)
    
    # Create document with category_id
    doc_data = {
        "category": item.get('category'),
        "category_id": category_id,  # ← Now included!
        # ... other fields
    }
    docs_to_insert.append(doc_data)

# Bulk insert
bulk_create_documents(db, docs_to_insert)
```

## Benefits

1. ✅ **Database Integrity**: Foreign key constraint ensures valid category references
2. ✅ **Automatic Management**: Categories auto-created when new names are encountered  
3. ✅ **Performance**: Index on category_id speeds up queries
4. ✅ **Backward Compatible**: Nullable field doesn't break existing data
5. ✅ **Java Integration**: Main backend can now properly filter by category_id

## Migration Path

```bash
# 1. Apply migration
cd crawling-server
alembic upgrade head

# 2. Restart services  
docker-compose restart celery-worker celery-beat

# 3. Verify
psql -d incheon_notice -c "SELECT * FROM detail_category;"
psql -d incheon_notice -c "SELECT id, category, category_id FROM crawl_notice ORDER BY id DESC LIMIT 5;"
```

## Testing

```bash
# Run unit tests
cd crawling-server
pytest tests/unit/test_category_id.py -v

# Test scenarios:
# ✓ Create new category
# ✓ Reuse existing category
# ✓ Handle null/empty category
# ✓ Document with category_id
# ✓ Bulk insert with category_id
# ✓ Backward compatibility
```
