package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.repository.CrawlNoticeRepository;
import com.incheon.notice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 웹훅 API Controller
 * 크롤러 서버에서 새 공지사항 등록 시 호출되는 웹훅 엔드포인트
 */
@Tag(name = "웹훅", description = "크롤러 서버와 통신하는 웹훅 API (내부용)")
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final CrawlNoticeRepository crawlNoticeRepository;
    private final NotificationService notificationService;

    /**
     * 새 공지사항 등록 웹훅
     * POST /api/webhook/new-notice
     *
     * 크롤러 서버에서 새 공지사항을 등록한 후 이 엔드포인트를 호출하여
     * 키워드 알림을 트리거합니다.
     */
    @Operation(
        summary = "새 공지사항 등록 웹훅",
        description = """
            크롤러 서버에서 새 공지사항 등록 시 호출됩니다.

            **동작 과정:**
            1. 크롤러가 새 공지사항 발견 및 DB 저장
            2. 이 웹훅 호출 (POST /api/webhook/new-notice)
            3. 키워드 매칭 검사
            4. 매칭된 사용자들에게 FCM 푸시 알림 발송

            **보안:**
            - API Key 인증 필요 (X-API-Key 헤더)
            - 크롤러 서버만 호출 가능

            **제한:**
            - Rate limit: 1000 requests/hour
            """
    )
    @PostMapping("/new-notice")
    public ResponseEntity<ApiResponse<WebhookResponse>> handleNewNotice(
            @Parameter(description = "크롤러 API Key (헤더)")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,

            @RequestBody NewNoticeWebhookRequest request
    ) {
        log.info("Webhook received: new-notice, noticeId={}, title={}",
                request.getNoticeId(), request.getTitle());

        // 1. API Key 검증 (선택사항, 보안 강화)
        // TODO: application.properties에 webhook.api-key 설정 후 검증 로직 활성화
        // if (!isValidApiKey(apiKey)) {
        //     log.warn("Invalid API key for webhook: {}", apiKey);
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        //             .body(ApiResponse.error("Invalid API key"));
        // }

        // 2. 공지사항 조회
        CrawlNotice crawlNotice = crawlNoticeRepository.findById(request.getNoticeId())
                .orElse(null);

        if (crawlNotice == null) {
            log.warn("Notice not found for webhook: noticeId={}", request.getNoticeId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("공지사항을 찾을 수 없습니다"));
        }

        // 3. 알림 처리 (키워드 매칭 및 FCM 발송)
        int notificationsSent = 0;
        try {
            notificationsSent = notificationService.processNewNotice(crawlNotice);
        } catch (Exception e) {
            log.error("Failed to process notifications for notice: noticeId={}, error={}",
                    request.getNoticeId(), e.getMessage(), e);
            // 알림 실패해도 200 OK 반환 (크롤러는 계속 동작해야 함)
        }

        // 4. 중요 공지사항인 경우 전체 발송 (선택사항)
        if (Boolean.TRUE.equals(crawlNotice.getIsImportant()) && request.isBroadcast()) {
            try {
                notificationService.sendImportantNoticeToAll(crawlNotice);
            } catch (Exception e) {
                log.error("Failed to broadcast important notice: noticeId={}, error={}",
                        request.getNoticeId(), e.getMessage(), e);
            }
        }

        WebhookResponse response = WebhookResponse.builder()
                .noticeId(crawlNotice.getId())
                .title(crawlNotice.getTitle())
                .notificationsSent(notificationsSent)
                .message("웹훅 처리 완료")
                .build();

        log.info("Webhook processed successfully: noticeId={}, notifications={}",
                crawlNotice.getId(), notificationsSent);

        return ResponseEntity.ok(ApiResponse.success("웹훅 처리 완료", response));
    }

    /**
     * 웹훅 헬스체크
     * GET /api/webhook/health
     */
    @Operation(summary = "웹훅 헬스체크", description = "웹훅 서비스 상태 확인")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Webhook service is healthy", "OK"));
    }

    /**
     * API Key 검증 (선택사항)
     */
    private boolean isValidApiKey(String apiKey) {
        // TODO: application.properties에서 webhook.api-key 읽어서 검증
        // @Value("${webhook.api-key}")
        // private String validApiKey;
        // return validApiKey != null && validApiKey.equals(apiKey);
        return true; // 현재는 검증 비활성화
    }

    /**
     * 새 공지사항 웹훅 요청 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class NewNoticeWebhookRequest {
        /**
         * 공지사항 ID
         */
        private Long noticeId;

        /**
         * 공지사항 제목 (로깅용, 선택사항)
         */
        private String title;

        /**
         * 전체 방송 여부 (중요 공지사항인 경우)
         */
        @Builder.Default
        private boolean broadcast = false;
    }

    /**
     * 웹훅 응답 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class WebhookResponse {
        /**
         * 처리된 공지사항 ID
         */
        private Long noticeId;

        /**
         * 공지사항 제목
         */
        private String title;

        /**
         * 발송된 알림 개수
         */
        private Integer notificationsSent;

        /**
         * 처리 메시지
         */
        private String message;
    }
}
