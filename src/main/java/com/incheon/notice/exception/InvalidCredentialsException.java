package com.incheon.notice.exception;

/**
 * 인증 실패 예외
 * HTTP 401 Unauthorized
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
