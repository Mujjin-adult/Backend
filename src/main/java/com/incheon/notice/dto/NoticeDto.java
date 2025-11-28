package com.incheon.notice.dto;

import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.entity.Category;
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
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private String source;
        private String author;
        private LocalDateTime publishedAt;
        private Integer viewCount;
        private Boolean isImportant;
        private Boolean isPinned;
        private Boolean bookmarked;  // 현재 사용자가 북마크했는지 여부

        /**
         * CrawlNotice 엔티티를 Response DTO로 변환
         */
        public static Response from(CrawlNotice notice) {
            return Response.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .url(notice.getUrl())
                    .categoryId(notice.getCategoryId())
                    .source(notice.getSource())
                    .author(notice.getAuthor() != null ? notice.getAuthor() : notice.getWriter())
                    .publishedAt(notice.getPublishedAt())
                    .viewCount(notice.getViewCount() != null ? notice.getViewCount() : 0)
                    .isImportant(notice.getIsImportant())
                    .isPinned(notice.getIsPinned())
                    .bookmarked(false)  // 기본값, 나중에 설정
                    .build();
        }

        /**
         * CrawlNotice와 Category를 함께 Response DTO로 변환
         */
        public static Response from(CrawlNotice notice, Category category) {
            Response response = from(notice);
            if (category != null) {
                response.setCategoryName(category.getName());
                response.setCategoryCode(category.getCode());
            }
            return response;
        }
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
        private Long categoryId;
        private CategoryDto.Response category;
        private String source;
        private String author;
        private LocalDateTime publishedAt;
        private Integer viewCount;
        private Boolean isImportant;
        private Boolean isPinned;
        private String attachments;
        private Boolean bookmarked;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        /**
         * CrawlNotice 엔티티를 DetailResponse DTO로 변환
         */
        public static DetailResponse from(CrawlNotice notice) {
            return DetailResponse.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .url(notice.getUrl())
                    .externalId(notice.getExternalId())
                    .categoryId(notice.getCategoryId())
                    .source(notice.getSource())
                    .author(notice.getAuthor() != null ? notice.getAuthor() : notice.getWriter())
                    .publishedAt(notice.getPublishedAt())
                    .viewCount(notice.getViewCount() != null ? notice.getViewCount() : 0)
                    .isImportant(notice.getIsImportant())
                    .isPinned(notice.getIsPinned())
                    .attachments(notice.getAttachments())
                    .bookmarked(false)  // 기본값, 나중에 설정
                    .createdAt(notice.getCreatedAt())
                    .updatedAt(notice.getUpdatedAt())
                    .build();
        }

        /**
         * CrawlNotice와 Category를 함께 DetailResponse DTO로 변환
         */
        public static DetailResponse from(CrawlNotice notice, Category category) {
            DetailResponse response = from(notice);
            if (category != null) {
                response.setCategory(CategoryDto.Response.builder()
                        .id(category.getId())
                        .code(category.getCode())
                        .name(category.getName())
                        .type(category.getType() != null ? category.getType().name() : null)
                        .url(category.getUrl())
                        .isActive(category.getIsActive())
                        .description(category.getDescription())
                        .build());
            }
            return response;
        }
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
