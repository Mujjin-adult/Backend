package com.incheon.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 북마크 관련 DTO
 */
public class BookmarkDto {

    /**
     * 북마크 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private Long noticeId;
        private LocalDateTime createdAt;
    }

    /**
     * 북마크 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotNull(message = "공지사항 ID는 필수입니다")
        private Long noticeId;
    }
}
