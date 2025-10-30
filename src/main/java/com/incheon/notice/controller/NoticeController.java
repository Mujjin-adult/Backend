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
import org.springframework.web.bind.annotation.*;

/**
 * 공지사항 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "공지사항 조회 API")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 전체 공지사항 목록 조회 (페이징)
     * GET /api/notices
     */
    @GetMapping
    @Operation(
            summary = "전체 공지사항 목록 조회",
            description = "모든 공지사항을 페이징하여 조회합니다. 최신 공지사항부터 정렬됩니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getAllNotices(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("전체 공지사항 목록 조회 API 호출: page={}, size={}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.getAllNotices(pageable);

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
     * GET /api/categories/{categoryCode}/notices
     */
    @GetMapping("/category/{categoryCode}")
    @Operation(
            summary = "카테고리별 공지사항 조회",
            description = "특정 카테고리의 공지사항 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<Page<NoticeDto.Response>>> getNoticesByCategory(
            @Parameter(description = "카테고리 코드", example = "SCHOLARSHIP")
            @PathVariable String categoryCode,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("카테고리별 공지사항 조회 API 호출: categoryCode={}, page={}, size={}",
                categoryCode, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NoticeDto.Response> notices = noticeService.getNoticesByCategoryCode(categoryCode, pageable);

            return ResponseEntity.ok(
                    ApiResponse.success("카테고리별 공지사항 조회 성공", notices)
            );
        } catch (RuntimeException e) {
            log.error("카테고리별 공지사항 조회 실패: categoryCode={}", categoryCode, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
