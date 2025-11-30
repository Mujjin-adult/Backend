package com.incheon.notice.exception;

/**
 * 비즈니스 로직 예외
 * HTTP 400 Bad Request
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
