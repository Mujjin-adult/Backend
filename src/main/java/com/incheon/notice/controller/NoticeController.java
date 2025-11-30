package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.NoticeDto;
import com.incheon.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공지사항 API Controller
 * 공지사항 조회 및 관리 기능
 */
@Tag(name = "공지사항", description = "공지사항 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 공지사항 목록 조회 (페이징, 필터링, 정렬)
     * GET /api/notices?page=0&size=20&categoryId=1&sortBy=latest&important=false
     */
    @Operation(
            summary = "공지사항 목록 조회",
            description = "공지사항 목록을 페이징하여 조회합니다. 카테고리, 중요 공지 필터링과 정렬 옵션을 제공합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getNotices(
            @Parameter(description = "카테고리 ID (선택사항)")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "정렬 방식 (latest: 최신순, oldest: 오래된순, popular: 인기순)")
            @RequestParam(defaultValue = "latest") String sortBy,

            @Parameter(description = "중요 공지만 조회 여부")
            @RequestParam(required = false) Boolean important,

            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/notices - categoryId: {}, sortBy: {}, important: {}, page: {}, size: {}",
                categoryId, sortBy, important, page, size);

        String userEmail = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);

        Page<NoticeDto.Response> notices = noticeService.getNotices(
                categoryId,
                sortBy,
                important,
                pageable,
                userEmail
        );

        return ResponseEntity.ok(ApiResponse.success("공지사항 목록 조회 성공", notices));
    }

    /**
     * 공지사항 상세 조회
     * GET /api/notices/{noticeId}
     */
    @Operation(
            summary = "공지사항 상세 조회",
            description = "특정 공지사항의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDto.DetailResponse>> getNoticeDetail(
            @Parameter(description = "공지사항 ID")
            @PathVariable Long noticeId
    ) {
        log.info("GET /api/notices/{} - fetching detail", noticeId);

        String userEmail = getCurrentUserEmail();
        NoticeDto.DetailResponse notice = noticeService.getNoticeDetail(noticeId, userEmail);

        return ResponseEntity.ok(ApiResponse.success("공지사항 조회 성공", notice));
    }

    /**
     * 관련 공지사항 조회
     * GET /api/notices/{noticeId}/related?limit=5
     */
    @Operation(
            summary = "관련 공지사항 조회",
            description = "특정 공지사항과 같은 카테고리의 다른 공지사항을 조회합니다."
    )
    @GetMapping("/{noticeId}/related")
    public ResponseEntity<ApiResponse<List<NoticeDto.Response>>> getRelatedNotices(
            @Parameter(description = "기준 공지사항 ID")
            @PathVariable Long noticeId,

            @Parameter(description = "조회할 관련 공지사항 개수")
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("GET /api/notices/{}/related - limit: {}", noticeId, limit);

        List<NoticeDto.Response> relatedNotices = noticeService.getRelatedNotices(noticeId, limit);

        return ResponseEntity.ok(ApiResponse.success("관련 공지사항 조회 성공", relatedNotices));
    }

    /**
     * 중요 공지사항 목록 조회
     * GET /api/notices/important
     */
    @Operation(
            summary = "중요 공지사항 목록 조회",
            description = "중요 표시된 공지사항 목록을 조회합니다."
    )
    @GetMapping("/important")
    public ResponseEntity<ApiResponse<List<NoticeDto.Response>>> getImportantNotices() {
        log.info("GET /api/notices/important");

        String userEmail = getCurrentUserEmail();
        List<NoticeDto.Response> importantNotices = noticeService.getImportantNotices(userEmail);

        return ResponseEntity.ok(ApiResponse.success("중요 공지사항 조회 성공", importantNotices));
    }

    /**
     * 현재 인증된 사용자의 이메일 가져오기 (null 가능)
     */
    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !authentication.getPrincipal().equals("anonymousUser")) {
                return authentication.getName();  // Firebase에서는 email이 name으로 설정됨
            }
        } catch (Exception e) {
            log.warn("Failed to get current user email: {}", e.getMessage());
        }
        return null;
    }
}
