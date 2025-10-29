package com.incheon.notice.dto;

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
        private NoticeDto.Response notice;  // 북마크된 공지사항 정보
        private String memo;
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

        private Long noticeId;
        private String memo;  // 선택사항
    }

    /**
     * 북마크 메모 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        private String memo;
    }
}
