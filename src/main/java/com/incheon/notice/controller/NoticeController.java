package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.NoticeDto;
import com.incheon.notice.security.CustomUserDetailsService;
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

/**
 * 공지사항 API Controller
 */
@Tag(name = "공지사항", description = "공지사항 조회 및 검색 API")
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 전체 공지사항 목록 조회 (페이징)
     * GET /api/notices
     */
    @GetMapping
    @Operation(
            summary = "전체 공지사항 목록 조회",
            description = "모든 공지사항을 페이징하여 조회합니다. 정렬 방식을 선택할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getAllNotices(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 방식 (latest: 최신순, viewCount: 조회순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort) {

        log.info("전체 공지사항 목록 조회 API 호출: page={}, size={}, sort={}", page, size, sort);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.getAllNotices(pageable, sort);

            return ResponseEntity.ok(
                    ApiResponse.success("공지사항 목록 조회 성공", notices)
            );
        } catch (Exception e) {
            log.error("공지사항 목록 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 공지사항 상세 조회
     * GET /api/notices/{id}
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "ID로 특정 공지사항의 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<NoticeDto.DetailResponse>> getNoticeById(
            @Parameter(description = "공지사항 ID", example = "1")
            @PathVariable Long id) {

        log.info("공지사항 상세 조회 API 호출: id={}", id);

        try {
            NoticeDto.DetailResponse notice = noticeService.getNoticeById(id);

            return ResponseEntity.ok(
                    ApiResponse.success("공지사항 상세 조회 성공", notice)
            );
        } catch (RuntimeException e) {
            log.error("공지사항 상세 조회 실패: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 공지사항 검색
     * GET /api/notices/search
     */
    @GetMapping("/search")
    @Operation(
            summary = "공지사항 검색",
            description = "제목 또는 내용에 키워드가 포함된 공지사항을 검색합니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> searchNotices(
            @Parameter(description = "검색 키워드", example = "장학금")
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("공지사항 검색 API 호출: keyword={}, page={}, size={}", keyword, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.searchNotices(keyword, pageable);

            return ResponseEntity.ok(
                    ApiResponse.success("공지사항 검색 성공", notices)
            );
        } catch (Exception e) {
            log.error("공지사항 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 카테고리별 공지사항 조회
     * GET /api/notices/category/{categoryCode}
     */
    @GetMapping("/category/{categoryCode}")
    @Operation(
            summary = "카테고리별 공지사항 조회",
            description = "특정 카테고리의 공지사항 목록을 조회합니다. 정렬 방식을 선택할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getNoticesByCategory(
            @Parameter(description = "카테고리 코드", example = "SCHOLARSHIP")
            @PathVariable String categoryCode,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 방식 (latest: 최신순, viewCount: 조회순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort) {

        log.info("카테고리별 공지사항 조회 API 호출: categoryCode={}, page={}, size={}, sort={}",
                categoryCode, page, size, sort);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.getNoticesByCategoryCode(categoryCode, pageable, sort);

            return ResponseEntity.ok(
                    ApiResponse.success("카테고리별 공지사항 조회 성공", notices)
            );
        } catch (RuntimeException e) {
            log.error("카테고리별 공지사항 조회 실패: categoryCode={}", categoryCode, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 구독한 카테고리의 공지사항 조회
     * GET /api/notices/subscribed
     */
    @GetMapping("/subscribed")
    @Operation(
            summary = "구독 공지사항 조회",
            description = "사용자가 구독한 카테고리의 공지사항만 조회합니다. 정렬 방식을 선택할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getSubscribedNotices(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 방식 (latest: 최신순, viewCount: 조회순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort) {

        log.info("구독 공지사항 조회 API 호출: page={}, size={}, sort={}", page, size, sort);

        try {
            Long userId = getCurrentUserId();
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.getSubscribedNotices(userId, pageable, sort);

            return ResponseEntity.ok(
                    ApiResponse.success("구독 공지사항 조회 성공", notices)
            );
        } catch (RuntimeException e) {
            log.error("구독 공지사항 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
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
