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
    @Operation(summary = "북마크 생성", description = "공지사항을 북마크에 저장합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkDto.Response>> createBookmark(
            @Valid @RequestBody BookmarkDto.CreateRequest request) {

        Long userId = getCurrentUserId();
        BookmarkDto.Response bookmark = bookmarkService.createBookmark(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("북마크가 생성되었습니다", bookmark));
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
