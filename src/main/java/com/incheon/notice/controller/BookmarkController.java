package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.BookmarkDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 북마크 API Controller
 * 사용자가 공지사항을 북마크하고 관리하는 기능
 */
@Tag(name = "북마크", description = "공지사항 저장 및 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 북마크 생성 (공지사항 저장)
     * POST /api/bookmarks
     */
    @Operation(summary = "북마크 생성", description = "공지사항을 북마크에 저장합니다. 선택적으로 메모를 추가할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkDto.Response>> createBookmark(
            @Valid @RequestBody BookmarkDto.CreateRequest request) {

        Long userId = getCurrentUserId();
        BookmarkDto.Response bookmark = bookmarkService.createBookmark(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("북마크가 생성되었습니다", bookmark));
    }

    /**
     * 내 북마크 목록 조회
     * GET /api/bookmarks?page=0&size=20
     */
    @Operation(summary = "북마크 목록 조회", description = "내가 저장한 북마크 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookmarkDto.Response>>> getMyBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<BookmarkDto.Response> bookmarks = bookmarkService.getMyBookmarks(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("북마크 목록 조회 성공", bookmarks));
    }

    /**
     * 북마크 상세 조회
     * GET /api/bookmarks/{id}
     */
    @Operation(summary = "북마크 상세 조회", description = "특정 북마크의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookmarkDto.Response>> getBookmark(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        BookmarkDto.Response bookmark = bookmarkService.getBookmark(userId, id);

        return ResponseEntity.ok(ApiResponse.success(bookmark));
    }

    /**
     * 북마크 메모 수정
     * PUT /api/bookmarks/{id}/memo
     */
    @Operation(summary = "북마크 메모 수정", description = "저장한 북마크의 메모를 수정합니다.")
    @PutMapping("/{id}/memo")
    public ResponseEntity<ApiResponse<BookmarkDto.Response>> updateBookmarkMemo(
            @PathVariable Long id,
            @Valid @RequestBody BookmarkDto.UpdateRequest request) {

        Long userId = getCurrentUserId();
        BookmarkDto.Response bookmark = bookmarkService.updateBookmarkMemo(userId, id, request);

        return ResponseEntity.ok(ApiResponse.success("메모가 수정되었습니다", bookmark));
    }

    /**
     * 북마크 삭제
     * DELETE /api/bookmarks/{id}
     */
    @Operation(summary = "북마크 삭제", description = "저장한 북마크를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        bookmarkService.deleteBookmark(userId, id);

        return ResponseEntity.ok(ApiResponse.success("북마크가 삭제되었습니다", null));
    }

    /**
     * 특정 공지사항 북마크 여부 확인
     * GET /api/bookmarks/check/{noticeId}
     */
    @Operation(summary = "북마크 여부 확인", description = "특정 공지사항이 북마크되어 있는지 확인합니다.")
    @GetMapping("/check/{noticeId}")
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(@PathVariable Long noticeId) {
        Long userId = getCurrentUserId();
        boolean isBookmarked = bookmarkService.isBookmarked(userId, noticeId);

        return ResponseEntity.ok(ApiResponse.success(isBookmarked));
    }

    /**
     * 내 북마크 개수
     * GET /api/bookmarks/count
     */
    @Operation(summary = "북마크 개수 조회", description = "내가 저장한 북마크의 총 개수를 조회합니다.")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getBookmarkCount() {
        Long userId = getCurrentUserId();
        long count = bookmarkService.getBookmarkCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count));
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
