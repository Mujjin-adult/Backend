package com.incheon.notice.service;

import com.incheon.notice.dto.SearchDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.repository.BookmarkRepository;
import com.incheon.notice.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 검색 서비스
 * PostgreSQL Full-Text Search 기능 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;
    private final CategoryRepository categoryRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 전문 검색 (Full-Text Search)
     *
     * PostgreSQL의 tsvector, tsquery, ts_rank, ts_headline 사용
     *
     * @param request 검색 요청 (키워드, 필터, 페이징)
     * @param userEmail 현재 사용자 이메일 (북마크 확인용, null 가능)
     * @return 검색 결과 (하이라이트, 관련도 점수 포함)
     */
    @Transactional(readOnly = true)
    public SearchDto.SearchResponse search(SearchDto.SearchRequest request, String userEmail) {
        long startTime = System.currentTimeMillis();

        // 1. 검색 키워드 검증 및 전처리
        String keyword = sanitizeKeyword(request.getKeyword());
        if (keyword == null || keyword.trim().isEmpty()) {
            return buildEmptyResponse(request, 0L);
        }

        // 2. tsquery 생성 (OR 검색 지원)
        String tsquery = buildTsQuery(keyword);
        log.debug("Search keyword: '{}' -> tsquery: '{}'", keyword, tsquery);

        // 3. 검색 쿼리 실행 (카운트 + 결과)
        Long totalCount = executeCountQuery(tsquery, request.getCategoryId());
        List<SearchDto.SearchResult> results = executeSearchQuery(
                tsquery,
                request.getCategoryId(),
                request.getSortBy(),
                request.getPage(),
                request.getSize()
        );

        // 4. 카테고리 정보 채우기
        enrichWithCategoryInfo(results);

        // 5. 북마크 정보 채우기 (로그인 사용자만)
        if (userEmail != null) {
            enrichWithBookmarkInfo(results, userEmail);
        }

        // 6. 페이징 정보 계산
        int totalPages = (int) Math.ceil((double) totalCount / request.getSize());
        long searchTimeMs = System.currentTimeMillis() - startTime;

        log.info("Search completed: keyword='{}', results={}, totalCount={}, timeMs={}",
                keyword, results.size(), totalCount, searchTimeMs);

        return SearchDto.SearchResponse.builder()
                .results(results)
                .keyword(keyword)
                .totalCount(totalCount)
                .currentPage(request.getPage())
                .pageSize(request.getSize())
                .totalPages(totalPages)
                .hasNext(request.getPage() + 1 < totalPages)
                .hasPrevious(request.getPage() > 0)
                .searchTimeMs(searchTimeMs)
                .build();
    }

    /**
     * 검색 키워드 sanitize (SQL Injection 방지)
     */
    private String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        // 특수문자 제거, 공백 정리
        return keyword
                .trim()
                .replaceAll("[;'\"\\\\]", "") // SQL 특수문자 제거
                .replaceAll("\\s+", " ");     // 연속 공백을 하나로
    }

    /**
     * tsquery 생성 (OR 검색)
     * 예: "장학금 학사" -> "장학금 | 학사"
     */
    private String buildTsQuery(String keyword) {
        String[] words = keyword.trim().split("\\s+");
        if (words.length == 1) {
            return words[0];
        }
        // 여러 단어를 OR로 연결
        return String.join(" | ", words);
    }

    /**
     * 검색 결과 개수 조회
     */
    private Long executeCountQuery(String tsquery, Long categoryId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM crawl_notice ");
        sql.append("WHERE search_vector @@ to_tsquery('simple', :tsquery) ");

        if (categoryId != null) {
            sql.append("AND category_id = :categoryId ");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("tsquery", tsquery);

        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * 검색 쿼리 실행 (ts_rank, ts_headline 사용)
     */
    @SuppressWarnings("unchecked")
    private List<SearchDto.SearchResult> executeSearchQuery(
            String tsquery,
            Long categoryId,
            String sortBy,
            int page,
            int size
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  id, ");
        sql.append("  ts_headline('simple', title, to_tsquery('simple', :tsquery), ");
        sql.append("    'StartSel=<mark>, StopSel=</mark>, MaxWords=10, MinWords=5') AS highlighted_title, ");
        sql.append("  ts_headline('simple', COALESCE(content, ''), to_tsquery('simple', :tsquery), ");
        sql.append("    'StartSel=<mark>, StopSel=</mark>, MaxWords=35, MinWords=15, MaxFragments=1') AS highlighted_content, ");
        sql.append("  url, ");
        sql.append("  category_id, ");
        sql.append("  source, ");
        sql.append("  COALESCE(author, writer) AS author, ");
        sql.append("  published_at, ");
        sql.append("  COALESCE(view_count, 0) AS view_count, ");
        sql.append("  is_important, ");
        sql.append("  ts_rank(search_vector, to_tsquery('simple', :tsquery)) AS relevance_score ");
        sql.append("FROM crawl_notice ");
        sql.append("WHERE search_vector @@ to_tsquery('simple', :tsquery) ");

        if (categoryId != null) {
            sql.append("AND category_id = :categoryId ");
        }

        // 정렬
        sql.append("ORDER BY ");
        if ("latest".equals(sortBy)) {
            sql.append("published_at DESC NULLS LAST ");
        } else if ("oldest".equals(sortBy)) {
            sql.append("published_at ASC NULLS LAST ");
        } else { // relevance (기본값)
            sql.append("relevance_score DESC, published_at DESC NULLS LAST ");
        }

        // 페이징
        sql.append("LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("tsquery", tsquery);

        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }

        query.setParameter("limit", size);
        query.setParameter("offset", page * size);

        List<Object[]> rows = query.getResultList();

        // Object[] -> SearchResult 변환
        List<SearchDto.SearchResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            SearchDto.SearchResult result = SearchDto.SearchResult.builder()
                    .id(((Number) row[0]).longValue())
                    .title((String) row[1])  // highlighted_title
                    .contentPreview((String) row[2])  // highlighted_content
                    .url((String) row[3])
                    .categoryId(row[4] != null ? ((Number) row[4]).longValue() : null)
                    .source((String) row[5])
                    .author((String) row[6])
                    .publishedAt(row[7] != null ? ((Timestamp) row[7]).toLocalDateTime() : null)
                    .viewCount(((Number) row[8]).intValue())
                    .isImportant((Boolean) row[9])
                    .relevanceScore(row[10] != null ? ((Number) row[10]).doubleValue() : 0.0)
                    .bookmarked(false)
                    .build();

            results.add(result);
        }

        return results;
    }

    /**
     * 카테고리 정보 채우기
     */
    private void enrichWithCategoryInfo(List<SearchDto.SearchResult> results) {
        // categoryId가 있는 결과만 추출
        List<Long> categoryIds = results.stream()
                .map(SearchDto.SearchResult::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (categoryIds.isEmpty()) {
            return;
        }

        // 카테고리 정보 일괄 조회
        Map<Long, String> categoryMap = categoryRepository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 결과에 카테고리 이름 설정
        results.forEach(result -> {
            if (result.getCategoryId() != null) {
                result.setCategoryName(categoryMap.get(result.getCategoryId()));
            }
        });
    }

    /**
     * 북마크 정보 채우기 (로그인 사용자만)
     */
    private void enrichWithBookmarkInfo(List<SearchDto.SearchResult> results, String userEmail) {
        for (SearchDto.SearchResult result : results) {
            boolean isBookmarked = bookmarkRepository
                    .existsByUser_EmailAndCrawlNotice_Id(userEmail, result.getId());
            result.setBookmarked(isBookmarked);
        }
    }

    /**
     * 빈 검색 결과 생성
     */
    private SearchDto.SearchResponse buildEmptyResponse(SearchDto.SearchRequest request, long totalCount) {
        return SearchDto.SearchResponse.builder()
                .results(new ArrayList<>())
                .keyword(request.getKeyword())
                .totalCount(totalCount)
                .currentPage(request.getPage())
                .pageSize(request.getSize())
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .searchTimeMs(0L)
                .build();
    }

    /**
     * 인기 검색어 조회 (최근 24시간 기준, TOP 10)
     *
     * Note: 실제 구현을 위해서는 별도 search_log 테이블이 필요합니다.
     * 현재는 placeholder로 빈 리스트 반환
     */
    @Transactional(readOnly = true)
    public List<SearchDto.PopularKeyword> getPopularKeywords(int limit) {
        // TODO: search_log 테이블 구현 후 실제 통계 쿼리로 대체
        log.warn("Popular keywords not implemented yet - search_log table required");
        return new ArrayList<>();
    }

    /**
     * 검색어 자동완성 (prefix matching)
     *
     * @param prefix 입력 중인 검색어 (예: "장학")
     * @param limit 결과 개수 제한
     * @return 자동완성 제안 목록
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SearchDto.AutocompleteSuggestion> autocomplete(String prefix, int limit) {
        if (prefix == null || prefix.trim().length() < 2) {
            return new ArrayList<>();
        }

        String sanitized = sanitizeKeyword(prefix);

        // prefix:* 를 사용한 접두사 검색
        String tsquery = sanitized + ":*";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  title, ");
        sql.append("  COUNT(*) AS match_count ");
        sql.append("FROM crawl_notice ");
        sql.append("WHERE search_vector @@ to_tsquery('simple', :tsquery) ");
        sql.append("GROUP BY title ");
        sql.append("ORDER BY match_count DESC ");
        sql.append("LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("tsquery", tsquery);
        query.setParameter("limit", limit);

        List<Object[]> rows = query.getResultList();

        List<SearchDto.AutocompleteSuggestion> suggestions = new ArrayList<>();
        for (Object[] row : rows) {
            suggestions.add(SearchDto.AutocompleteSuggestion.builder()
                    .keyword((String) row[0])
                    .matchCount(((Number) row[1]).longValue())
                    .build());
        }

        return suggestions;
    }
}
