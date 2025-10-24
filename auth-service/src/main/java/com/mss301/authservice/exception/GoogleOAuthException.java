package com.mss301.authservice.exception;

/**
 * Custom exception for Google OAuth operations
 * Follows Single Responsibility Principle (SRP)
 */
public class GoogleOAuthException extends AppException {

    public GoogleOAuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GoogleOAuthException(ErrorCode errorCode, String message) {
        super(errorCode);
    }

    public GoogleOAuthException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode);
    }
}
