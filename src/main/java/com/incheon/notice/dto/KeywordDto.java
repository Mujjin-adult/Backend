package com.incheon.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 키워드 관련 DTO
 */
public class KeywordDto {

    /**
     * 키워드 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String keyword;
        private Long categoryId;
        private String categoryName;
        private Boolean isActive;
        private Integer matchedCount;
        private LocalDateTime lastNotifiedAt;
        private LocalDateTime createdAt;
    }

    /**
     * 키워드 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "키워드 등록 요청")
    public static class CreateRequest {

        @Schema(description = "알림 받을 키워드 (# 기호는 자동 제거됨)", example = "장학금", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "키워드는 필수입니다")
        @Size(min = 1, max = 50, message = "키워드는 1~50자 사이여야 합니다")
        private String keyword;
    }

}
