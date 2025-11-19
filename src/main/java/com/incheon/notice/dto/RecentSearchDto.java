package com.incheon.notice.dto;

import com.incheon.notice.entity.RecentSearch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 최근 검색어 DTO
 */
public class RecentSearchDto {

    /**
     * 최근 검색어 저장 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveRequest {
        private String keyword;  // 검색 키워드
    }

    /**
     * 최근 검색어 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String keyword;
        private LocalDateTime searchedAt;

        /**
         * Entity -> DTO 변환
         */
        public static Response from(RecentSearch recentSearch) {
            return Response.builder()
                    .id(recentSearch.getId())
                    .keyword(recentSearch.getKeyword())
                    .searchedAt(recentSearch.getCreatedAt())
                    .build();
        }
    }
}
