package com.incheon.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 크롤러 API DTO
 * crawling-server와 데이터 주고받기 위한 DTO
 */
public class CrawlerDto {

    /**
     * 크롤링된 공지사항 데이터 수신 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "크롤링된 공지사항 데이터")
    public static class NoticeRequest {

        @NotBlank(message = "제목은 필수입니다")
        @Schema(description = "공지사항 제목", example = "2024학년도 1학기 장학금 신청 안내")
        private String title;

        @Schema(description = "공지사항 내용", example = "장학금 신청 기간은...")
        private String content;

        @NotBlank(message = "URL은 필수입니다")
        @Schema(description = "원본 URL", example = "https://www.inu.ac.kr/bbs/inu/253/12345/artclView.do")
        private String url;

        @NotBlank(message = "외부 ID는 필수입니다")
        @Schema(description = "외부 시스템 ID (중복 방지용)", example = "abc123def456")
        private String externalId;

        @NotBlank(message = "카테고리 코드는 필수입니다")
        @Schema(description = "카테고리 코드", example = "SCHOLARSHIP")
        private String categoryCode;

        @Schema(description = "작성자", example = "학생지원팀")
        private String author;

        @Schema(description = "게시일", example = "2024-10-28T10:30:00")
        private LocalDateTime publishedAt;

        @Schema(description = "조회수", example = "123")
        private Integer viewCount;

        @Schema(description = "중요 공지 여부", example = "false")
        private Boolean isImportant;

        @Schema(description = "첨부파일 정보", example = "첨부파일1.pdf, 첨부파일2.hwp")
        private String attachments;

        @Schema(description = "크롤링 소스", example = "volunteer")
        private String source;
    }

    /**
     * 크롤링 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "크롤링 결과 응답")
    public static class NoticeResponse {

        @Schema(description = "공지사항 ID")
        private Long id;

        @Schema(description = "제목")
        private String title;

        @Schema(description = "외부 ID")
        private String externalId;

        @Schema(description = "카테고리 코드")
        private String categoryCode;

        @Schema(description = "생성 여부 (true: 신규, false: 기존)")
        private Boolean created;

        @Schema(description = "메시지")
        private String message;
    }

    /**
     * 일괄 크롤링 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일괄 크롤링 요청")
    public static class BulkCrawlRequest {

        @NotBlank(message = "카테고리 코드는 필수입니다")
        @Schema(description = "크롤링할 카테고리 코드 (all: 전체)", example = "SCHOLARSHIP")
        private String categoryCode;

        @Schema(description = "최대 페이지 수", example = "5")
        private Integer maxPages;
    }

    /**
     * 크롤링 상태 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "크롤링 상태")
    public static class CrawlStatusResponse {

        @Schema(description = "상태", example = "RUNNING")
        private String status;

        @Schema(description = "진행률 (%)", example = "65")
        private Integer progress;

        @Schema(description = "크롤링된 문서 수", example = "123")
        private Integer crawledCount;

        @Schema(description = "메시지")
        private String message;
    }
}
