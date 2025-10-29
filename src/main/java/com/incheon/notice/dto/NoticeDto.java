package com.incheon.notice.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 공지사항 관련 DTO
 */
public class NoticeDto {

    /**
     * 공지사항 응답 DTO (목록용 - 간단한 정보)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String title;
        private String url;
        private String categoryName;
        private String categoryCode;
        private String author;
        private LocalDateTime publishedAt;
        private Integer viewCount;
        private Boolean isImportant;
        private Boolean isPinned;
        private Boolean isBookmarked;  // 현재 사용자가 북마크했는지 여부
    }

    /**
     * 공지사항 상세 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {

        private Long id;
        private String title;
        private String content;
        private String url;
        private String externalId;
        private CategoryDto.Response category;
        private String author;
        private LocalDateTime publishedAt;
        private Integer viewCount;
        private Boolean isImportant;
        private Boolean isPinned;
        private String attachments;
        private Boolean isBookmarked;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 공지사항 생성 요청 DTO (크롤러에서 전송)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        private String title;
        private String content;
        private String url;
        private String externalId;
        private String categoryCode;  // 카테고리 코드
        private String author;
        private LocalDateTime publishedAt;
        private Integer viewCount;
        private Boolean isImportant;
        private String attachments;
    }

    /**
     * 공지사항 검색 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {

        private String keyword;  // 검색 키워드
        private Long categoryId;  // 카테고리 필터 (선택사항)
        private LocalDateTime startDate;  // 시작일 (선택사항)
        private LocalDateTime endDate;  // 종료일 (선택사항)
        private int page = 0;
        private int size = 20;
    }
}
