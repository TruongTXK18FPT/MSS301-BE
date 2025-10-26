package com.mss301.profileservice.exception;

/**
 * Custom business exception for profile service
 * Represents business rule violations
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
