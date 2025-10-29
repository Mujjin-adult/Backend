package com.incheon.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 카테고리 관련 DTO
 */
public class CategoryDto {

    /**
     * 카테고리 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String code;
        private String name;
        private String type;  // DEPARTMENT, COLLEGE, GRADUATE, ADMINISTRATIVE
        private String url;
        private Boolean isActive;
        private String description;
        private Integer noticeCount;  // 해당 카테고리의 공지사항 개수 (선택적)
    }

    /**
     * 카테고리 생성 요청 DTO (관리자용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "카테고리 코드는 필수입니다")
        private String code;

        @NotBlank(message = "카테고리 이름은 필수입니다")
        private String name;

        @NotBlank(message = "카테고리 유형은 필수입니다")
        private String type;  // DEPARTMENT, COLLEGE, GRADUATE, ADMINISTRATIVE

        private String url;
        private String description;
    }

    /**
     * 카테고리 수정 요청 DTO (관리자용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        private String name;
        private String url;
        private Boolean isActive;
        private String description;
    }
}
