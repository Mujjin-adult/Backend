package com.incheon.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 알림 설정 관련 DTO
 */
public class UserPreferenceDto {

    /**
     * 사용자 알림 설정 응답 DTO
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
     * 카테고리 구독 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeRequest {

        @NotNull(message = "카테고리 ID는 필수입니다")
        private Long categoryId;

        private Boolean notificationEnabled = true;  // 기본값: 알림 활성화
    }

    /**
     * 알림 설정 변경 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNotificationRequest {

        @NotNull(message = "알림 설정 값은 필수입니다")
        private Boolean notificationEnabled;
    }
}
