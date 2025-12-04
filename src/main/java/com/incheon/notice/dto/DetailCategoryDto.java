package com.incheon.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * 상세 카테고리 관련 DTO
 */
public class DetailCategoryDto {

    /**
     * 상세 카테고리 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String name;
        private String description;
        private Boolean isActive;
        private Boolean subscribed;  // 사용자의 구독 여부
    }

    /**
     * 상세 카테고리 목록 응답 DTO (구독 정보 포함)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {

        private List<Response> categories;
    }

    /**
     * 상세 카테고리 구독 설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionRequest {

        @NotNull(message = "카테고리 ID는 필수입니다")
        private Long categoryId;

        @NotNull(message = "구독 여부는 필수입니다")
        private Boolean subscribed;
    }

    /**
     * 다중 상세 카테고리 구독 설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSubscriptionRequest {

        private List<SubscriptionRequest> subscriptions;
    }
}
