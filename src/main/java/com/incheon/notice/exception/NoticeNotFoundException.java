package com.incheon.notice.exception;

/**
 * 공지사항을 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 처리됨
 */
public class NoticeNotFoundException extends RuntimeException {

    public NoticeNotFoundException(Long noticeId) {
        super("공지사항을 찾을 수 없습니다 (ID: " + noticeId + ")");
    }

    public NoticeNotFoundException(String message) {
        super(message);
    }
}
