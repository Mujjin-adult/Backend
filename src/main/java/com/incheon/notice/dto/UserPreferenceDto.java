package com.incheon.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 알림 설정 관련 DTO
 */
public class UserPreferenceDto {

    /**
     * 사용자 알림 설정 응답 DTO (기존 카테고리 기반)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private CategoryDto.Response category;  // 카테고리 정보
        private Boolean notificationEnabled;  // 알림 활성화 여부
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 상세 카테고리 응답 DTO (사용자 구독 상태 포함)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailCategoryResponse {

        private Long id;
        private String name;           // 상세 카테고리명
        private Boolean subscribed;    // 사용자 구독 여부
    }

    /**
     * 상세 카테고리 구독 설정 요청 DTO
     * PATCH /api/preferences/categories
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailCategorySubscribeRequest {

        @NotNull(message = "구독 설정 목록은 필수입니다")
        private List<DetailCategorySubscription> subscriptions;
    }

    /**
     * 개별 상세 카테고리 구독 설정
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailCategorySubscription {

        @NotNull(message = "상세 카테고리 ID는 필수입니다")
        private Long detailCategoryId;

        @NotNull(message = "활성화 여부는 필수입니다")
        private Boolean enabled;
    }
}
