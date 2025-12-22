package com.incheon.notice.exception;

/**
 * 중복 리소스 예외
 * HTTP 409 Conflict
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
