package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.CrawlerDto;
import com.incheon.notice.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 크롤러 API Controller
 * crawling-server와의 통신을 담당
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Tag(name = "Crawler API", description = "크롤링 서버 연동 API")
public class CrawlerController {

    private final CrawlerService crawlerService;

    /**
     * 크롤링된 공지사항 수신 및 저장
     * POST /api/crawler/notices
     */
    @PostMapping("/notices")
    @Operation(
            summary = "크롤링된 공지사항 저장",
            description = "crawling-server로부터 크롤링된 공지사항 데이터를 받아 저장합니다. " +
                    "externalId를 기준으로 중복을 체크하며, 중복인 경우 업데이트합니다."
    )
    public ResponseEntity<ApiResponse<CrawlerDto.NoticeResponse>> receiveNotice(
            @Valid @RequestBody CrawlerDto.NoticeRequest request) {

        log.info("크롤링 데이터 수신 API 호출: title={}, categoryCode={}",
                request.getTitle(), request.getCategoryCode());

        try {
            CrawlerDto.NoticeResponse response = crawlerService.saveNotice(request);

            String message = response.getCreated()
                    ? "새 공지사항이 저장되었습니다"
                    : "기존 공지사항이 업데이트되었습니다";

            return ResponseEntity
                    .status(response.getCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                    .body(ApiResponse.success(message, response));

        } catch (RuntimeException e) {
            log.error("크롤링 데이터 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 크롤링 상태 조회
     * GET /api/crawler/status
     */
    @GetMapping("/status")
    @Operation(
            summary = "크롤링 상태 조회",
            description = "현재 크롤링 시스템의 상태를 조회합니다."
    )
    public ResponseEntity<ApiResponse<CrawlerDto.CrawlStatusResponse>> getCrawlStatus() {
        // TODO: 실제 크롤링 상태를 조회하는 로직 구현
        // 현재는 간단한 더미 응답 반환

        CrawlerDto.CrawlStatusResponse status = CrawlerDto.CrawlStatusResponse.builder()
                .status("READY")
                .progress(0)
                .crawledCount(0)
                .message("크롤링 시스템 준비 완료")
                .build();

        return ResponseEntity.ok(ApiResponse.success("상태 조회 성공", status));
    }

    /**
     * 헬스 체크
     * GET /api/crawler/health
     */
    @GetMapping("/health")
    @Operation(
            summary = "크롤러 API 헬스 체크",
            description = "크롤러 API가 정상 작동하는지 확인합니다."
    )
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("크롤러 API 정상 작동", "OK"));
    }
}
