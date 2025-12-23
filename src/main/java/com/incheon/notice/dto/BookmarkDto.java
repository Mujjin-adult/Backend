package com.incheon.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name = "BookmarkResponse", description = "북마크 응답")
    public static class Response {

        @Schema(description = "북마크 ID", example = "1")
        private Long id;

        @Schema(description = "북마크된 공지사항 정보")
        private NoticeDto.Response notice;

        @Schema(description = "북마크 생성 시간")
        private LocalDateTime createdAt;
    }

    /**
     * 북마크 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BookmarkCreateRequest", description = "북마크 생성 요청")
    public static class CreateRequest {

        @NotNull(message = "공지사항 ID는 필수입니다")
        @Schema(description = "북마크할 공지사항 ID", example = "123", required = true)
        private Long noticeId;
    }
}
