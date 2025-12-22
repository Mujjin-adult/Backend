# API 성능 최적화 가이드

## 개요

인천대학교 공지사항 알림 앱의 API 성능 최적화 방법을 정리한 문서입니다.

---

## 현재 성능 분석

### 주요 병목 지점

| 문제 | 심각도 | 현재 상태 | 영향 |
|------|--------|-----------|------|
| **N+1 쿼리 문제** | 심각 | 공지사항마다 개별 쿼리 | 20개 조회 시 41개 쿼리 발생 |
| **Redis 캐싱 미사용** | 높음 | 설정만 존재, 미적용 | 매 요청마다 DB 조회 |
| **카테고리 개별 조회** | 높음 | 공지사항마다 카테고리 조회 | 불필요한 DB 부하 |
| **북마크 개별 확인** | 중간 | 로그인 시 개별 확인 | 추가 쿼리 발생 |
| **복합 인덱스 미사용** | 중간 | 단일 인덱스만 존재 | 정렬+필터링 쿼리 비효율 |
| **전체 엔티티 조회** | 낮음 | 목록에서도 전체 필드 조회 | 불필요한 데이터 전송 |

### 현재 쿼리 흐름 (20개 공지사항 조회 시)

```
1. SELECT * FROM crawl_notice ... LIMIT 20    (1개 쿼리)
2. SELECT * FROM category WHERE id = ?        (20개 쿼리 - 각 공지사항마다)
3. SELECT EXISTS FROM bookmark WHERE ...      (20개 쿼리 - 로그인 사용자)
─────────────────────────────────────────────
총: 41개 쿼리
```

---

## 최적화 단계

### 1단계: 즉시 효과 (예상 50-70% 개선)

#### 1-1. N+1 쿼리 해결 - 배치 조회

**문제점:**
```java
// 현재: 공지사항마다 개별 조회
noticesPage.map(notice -> {
    categoryRepository.findById(notice.getCategoryId());  // N번 호출
    bookmarkRepository.existsByUser_EmailAndCrawlNotice_Id(...);  // N번 호출
});
```

**해결책:**
```java
// 개선: 한 번에 배치 조회
List<Long> categoryIds = notices.stream()
    .map(CrawlNotice::getCategoryId)
    .filter(Objects::nonNull)
    .distinct()
    .toList();

Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds)
    .stream()
    .collect(Collectors.toMap(Category::getId, Function.identity()));

// 북마크도 배치로 조회
Set<Long> bookmarkedIds = bookmarkRepository
    .findNoticeIdsByUserEmail(userEmail, noticeIds);
```

**개선 후 쿼리 흐름:**
```
1. SELECT * FROM crawl_notice ... LIMIT 20    (1개 쿼리)
2. SELECT * FROM category WHERE id IN (...)   (1개 쿼리)
3. SELECT notice_id FROM bookmark WHERE ...   (1개 쿼리)
─────────────────────────────────────────────
총: 3개 쿼리 (41개 → 3개, 93% 감소)
```

#### 1-2. Redis 캐싱 적용

**캐싱 대상:**
- 카테고리 목록 (거의 변경되지 않음)
- 인기 공지사항 (주기적 갱신)

**구현:**
```java
@Cacheable(value = "categories", key = "'all'")
public List<Category> getAllCategories() {
    return categoryRepository.findAll();
}

@Cacheable(value = "categoryMap", key = "'map'")
public Map<Long, Category> getCategoryMap() {
    return categoryRepository.findAll().stream()
        .collect(Collectors.toMap(Category::getId, Function.identity()));
}
```

**캐시 설정:**
- TTL: 10분 (이미 RedisConfig에 설정됨)
- 카테고리 변경 시 캐시 무효화

---

### 2단계: 중간 효과 (추가 20-30% 개선)

#### 2-1. 복합 인덱스 추가

```sql
-- 카테고리별 최신순 정렬 최적화
CREATE INDEX idx_category_published
ON crawl_notice (category_id, published_at DESC);

-- 중요 공지사항 정렬 최적화
CREATE INDEX idx_important_published
ON crawl_notice (is_important, published_at DESC);

-- 생성일 기준 정렬 최적화
CREATE INDEX idx_created_at
ON crawl_notice (created_at DESC);
```

#### 2-2. Projection 쿼리 사용

**문제점:** 목록 조회 시 content, raw 등 대용량 필드도 함께 조회

**해결책:**
```java
public interface NoticeListProjection {
    Long getId();
    String getTitle();
    String getUrl();
    Long getCategoryId();
    String getCategory();
    String getSource();
    String getAuthor();
    String getDate();
    LocalDateTime getPublishedAt();
    Integer getViewCount();
    String getHits();
    Boolean getIsImportant();
    Boolean getIsPinned();
}

@Query("SELECT n.id as id, n.title as title, ... FROM CrawlNotice n ...")
Page<NoticeListProjection> findAllProjected(Pageable pageable);
```

---

### 3단계: 추가 최적화

#### 3-1. 조회수 업데이트 최적화

**현재:** 전체 엔티티 로드 후 UPDATE
```java
notice.incrementViewCount();
crawlNoticeRepository.save(notice);  // 전체 엔티티 UPDATE
```

**개선:** 단일 UPDATE 쿼리
```java
@Modifying
@Query("UPDATE CrawlNotice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
void incrementViewCount(@Param("id") Long id);
```

#### 3-2. HTTP 응답 압축

**application.yml:**
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
```

#### 3-3. 페이지 크기 최적화

- 기본 페이지 크기: 20 → 15로 축소 검토
- 무한 스크롤 구현 시 10개씩 로드

---

## 예상 성능 개선

| 단계 | 적용 내용 | 예상 응답 시간 | 개선율 |
|------|-----------|----------------|--------|
| 현재 | - | 200-500ms | - |
| 1단계 | N+1 해결 + Redis 캐싱 | 50-100ms | 50-70% |
| 2단계 | + 인덱스 + Projection | 30-60ms | 추가 20% |
| 3단계 | + 조회수 최적화 + 압축 | 20-50ms | 추가 10% |

---

## 구현 체크리스트

### 1단계
- [x] BookmarkRepository에 배치 조회 메서드 추가
- [x] CategoryService에 캐싱 적용
- [x] NoticeService에서 배치 조회 사용

### 2단계 (예정)
- [ ] 복합 인덱스 마이그레이션 스크립트 작성
- [ ] NoticeListProjection 인터페이스 생성
- [ ] Repository에 Projection 쿼리 추가

### 3단계 (예정)
- [ ] 조회수 UPDATE 쿼리 최적화
- [ ] HTTP 압축 설정 추가
- [ ] 페이지 크기 최적화 검토

---

## 모니터링

### 성능 측정 방법

```bash
# API 응답 시간 측정
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/notices"

# curl-format.txt 내용:
#   time_total: %{time_total}s\n
#   time_connect: %{time_connect}s\n
#   time_starttransfer: %{time_starttransfer}s\n
```

### Actuator 메트릭

```
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/hikaricp.connections.active
```

---

## 참고 파일

- `NoticeService.java` - 공지사항 서비스 로직
- `NoticeController.java` - API 엔드포인트
- `CrawlNoticeRepository.java` - 데이터베이스 쿼리
- `BookmarkRepository.java` - 북마크 쿼리
- `CategoryService.java` - 카테고리 서비스 (캐싱 적용)
- `RedisConfig.java` - Redis 캐시 설정
