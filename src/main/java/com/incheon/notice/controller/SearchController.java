package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.RecentSearchDto;
import com.incheon.notice.dto.SearchDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.RecentSearchService;
import com.incheon.notice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 검색 API Controller
 * 전문 검색 (Full-Text Search) 및 최근 검색어 관리 기능
 */
@Tag(name = "검색", description = "전문 검색(Full-Text Search) 및 최근 검색어 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final RecentSearchService recentSearchService;

    /**
     * 전문 검색 (Full-Text Search)
     * GET /api/search?keyword=장학금&categoryId=1&sortBy=relevance&page=0&size=20
     */
    @Operation(
        summary = "공지사항 전문 검색",
        description = """
            PostgreSQL Full-Text Search를 사용한 고속 검색 기능입니다.

            **검색 기능:**
            - 제목 및 내용에서 키워드 검색
            - 여러 단어 입력 시 OR 검색 (예: "장학금 학사" → 장학금 OR 학사)
            - 검색어 하이라이트 (<mark> 태그)
            - 관련도 점수 기반 정렬 (ts_rank)

            **정렬 옵션:**
            - relevance: 관련도순 (기본값) - 검색어와 가장 관련있는 순서
            - latest: 최신순 - 게시일 기준 최신
            - oldest: 오래된순 - 게시일 기준 오래된

            **성능:**
            - GIN 인덱스 사용으로 LIKE 검색 대비 10-100배 빠름
            - 10,000건 기준: LIKE 200ms vs FTS 5ms
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SearchDto.SearchResponse>> search(
            @Parameter(description = "검색 키워드 (필수)", example = "장학금")
            @RequestParam String keyword,

            @Parameter(description = "카테고리 ID 필터 (선택사항)")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "정렬 방식 (relevance, latest, oldest)")
            @RequestParam(defaultValue = "relevance") String sortBy,

            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Search request: keyword='{}', categoryId={}, sortBy={}, page={}, size={}",
                keyword, categoryId, sortBy, page, size);

        String userEmail = getCurrentUserEmailOrNull();

        SearchDto.SearchRequest request = SearchDto.SearchRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        SearchDto.SearchResponse response = searchService.search(request, userEmail);

        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

    /**
     * 최근 검색어 저장
     * POST /api/search/recent
     */
    @Operation(summary = "최근 검색어 저장", description = "검색한 키워드를 최근 검색어에 저장합니다. 최대 5개까지 저장되며, 중복 키워드는 검색 시각이 갱신됩니다.")
    @PostMapping("/recent")
    public ResponseEntity<ApiResponse<RecentSearchDto.Response>> saveRecentSearch(
            @Valid @RequestBody RecentSearchDto.SaveRequest request) {

        Long userId = getCurrentUserId();
        RecentSearchDto.Response response = recentSearchService.saveRecentSearch(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("최근 검색어가 저장되었습니다", response));
    }

    /**
     * 최근 검색어 목록 조회
     * GET /api/search/recent
     */
    @Operation(summary = "최근 검색어 조회", description = "사용자의 최근 검색어 목록을 조회합니다. 최대 5개, 최신순으로 반환됩니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RecentSearchDto.Response>>> getRecentSearches() {
        Long userId = getCurrentUserId();
        List<RecentSearchDto.Response> searches = recentSearchService.getRecentSearches(userId);

        return ResponseEntity.ok(ApiResponse.success("최근 검색어 조회 성공", searches));
    }

    /**
     * 특정 최근 검색어 삭제
     * DELETE /api/search/recent/{id}
     */
    @Operation(summary = "최근 검색어 삭제", description = "특정 최근 검색어를 삭제합니다.")
    @DeleteMapping("/recent/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRecentSearch(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        recentSearchService.deleteRecentSearch(userId, id);

        return ResponseEntity.ok(ApiResponse.success("최근 검색어가 삭제되었습니다"));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID 가져오기
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증이 필요합니다");
        }

        return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUserId();
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 이메일 가져오기 (null 가능)
     * 검색은 비로그인 사용자도 사용 가능하므로 null을 허용합니다.
     */
    private String getCurrentUserEmailOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !authentication.getPrincipal().equals("anonymousUser")) {
                return authentication.getName();  // Firebase에서는 email이 name으로 설정됨
            }
        } catch (Exception e) {
            log.debug("Failed to get current user email: {}", e.getMessage());
        }
        return null;
    }
}
