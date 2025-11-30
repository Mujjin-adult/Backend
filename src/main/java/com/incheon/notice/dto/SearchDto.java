package com.incheon.notice.dto;

import com.incheon.notice.entity.CrawlNotice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 검색 관련 DTO
 * 전문 검색 (Full-Text Search) 요청 및 응답
 */
public class SearchDto {

    /**
     * 검색 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchRequest {
        /**
         * 검색 키워드 (필수)
         * 예: "장학금", "학사 일정"
         */
        private String keyword;

        /**
         * 카테고리 ID 필터 (선택사항)
         */
        private Long categoryId;

        /**
         * 검색 정렬 방식
         * - relevance: 관련도순 (기본값) - ts_rank 사용
         * - latest: 최신순 - published_at DESC
         * - oldest: 오래된순 - published_at ASC
         */
        @Builder.Default
        private String sortBy = "relevance";

        /**
         * 페이지 번호 (0부터 시작)
         */
        @Builder.Default
        private Integer page = 0;

        /**
         * 페이지 크기
         */
        @Builder.Default
        private Integer size = 20;
    }

    /**
     * 검색 결과 DTO (하이라이트 포함)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResult {
        /**
         * 공지사항 ID
         */
        private Long id;

        /**
         * 제목 (하이라이트 포함)
         * 예: "2024년 1학기 <mark>장학금</mark> 신청 안내"
         */
        private String title;

        /**
         * 내용 미리보기 (하이라이트 포함, 최대 200자)
         * 예: "...국가<mark>장학금</mark> 및 교내<mark>장학금</mark> 신청을 받습니다..."
         */
        private String contentPreview;

        /**
         * 원본 URL
         */
        private String url;

        /**
         * 카테고리 ID
         */
        private Long categoryId;

        /**
         * 카테고리 이름 (있는 경우)
         */
        private String categoryName;

        /**
         * 출처 (소스)
         */
        private String source;

        /**
         * 작성자
         */
        private String author;

        /**
         * 게시일
         */
        private LocalDateTime publishedAt;

        /**
         * 조회수
         */
        private Integer viewCount;

        /**
         * 중요 공지 여부
         */
        private Boolean isImportant;

        /**
         * 북마크 여부 (로그인 사용자만)
         */
        private Boolean bookmarked;

        /**
         * 검색 관련도 점수 (0.0 ~ 1.0)
         * ts_rank 정규화된 값
         */
        private Double relevanceScore;

        /**
         * CrawlNotice 엔티티에서 SearchResult 생성 (하이라이트 없음)
         */
        public static SearchResult from(CrawlNotice notice) {
            return SearchResult.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .contentPreview(truncateContent(notice.getContent(), 200))
                    .url(notice.getUrl())
                    .categoryId(notice.getCategoryId())
                    .source(notice.getSource())
                    .author(notice.getAuthor() != null ? notice.getAuthor() : notice.getWriter())
                    .publishedAt(notice.getPublishedAt())
                    .viewCount(notice.getViewCount() != null ? notice.getViewCount() : 0)
                    .isImportant(notice.getIsImportant())
                    .bookmarked(false)
                    .build();
        }

        /**
         * 내용 truncate (최대 길이로 자르기)
         */
        private static String truncateContent(String content, int maxLength) {
            if (content == null || content.isEmpty()) {
                return "";
            }
            if (content.length() <= maxLength) {
                return content;
            }
            return content.substring(0, maxLength) + "...";
        }
    }

    /**
     * 검색 응답 DTO (페이징 정보 포함)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResponse {
        /**
         * 검색 결과 목록
         */
        private List<SearchResult> results;

        /**
         * 검색 키워드
         */
        private String keyword;

        /**
         * 전체 결과 개수
         */
        private Long totalCount;

        /**
         * 현재 페이지 번호
         */
        private Integer currentPage;

        /**
         * 페이지 크기
         */
        private Integer pageSize;

        /**
         * 전체 페이지 수
         */
        private Integer totalPages;

        /**
         * 다음 페이지 존재 여부
         */
        private Boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         */
        private Boolean hasPrevious;

        /**
         * 검색 소요 시간 (ms)
         */
        private Long searchTimeMs;
    }

    /**
     * 인기 검색어 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PopularKeyword {
        /**
         * 검색어
         */
        private String keyword;

        /**
         * 검색 횟수
         */
        private Long searchCount;

        /**
         * 순위 (1위부터)
         */
        private Integer rank;
    }

    /**
     * 검색어 자동완성 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutocompleteSuggestion {
        /**
         * 제안 키워드
         */
        private String keyword;

        /**
         * 매칭된 공지사항 수
         */
        private Long matchCount;

        /**
         * 카테고리 정보 (있는 경우)
         */
        private String category;
    }
}
