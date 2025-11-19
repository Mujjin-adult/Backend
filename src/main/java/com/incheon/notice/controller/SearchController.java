package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.RecentSearchDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.RecentSearchService;
import io.swagger.v3.oas.annotations.Operation;
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
 * 최근 검색어 관리 기능
 */
@Tag(name = "검색", description = "검색 및 최근 검색어 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final RecentSearchService recentSearchService;

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
     * 모든 최근 검색어 삭제
     * DELETE /api/search/recent
     */
    @Operation(summary = "모든 최근 검색어 삭제", description = "사용자의 모든 최근 검색어를 삭제합니다.")
    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<String>> deleteAllRecentSearches() {
        Long userId = getCurrentUserId();
        recentSearchService.deleteAllRecentSearches(userId);

        return ResponseEntity.ok(ApiResponse.success("모든 최근 검색어가 삭제되었습니다"));
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
}
