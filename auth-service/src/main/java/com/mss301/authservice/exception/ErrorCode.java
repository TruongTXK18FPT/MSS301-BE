package com.mss301.authservice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "Email này đã được sử dụng. Vui lòng chọn email khác.", HttpStatus.BAD_REQUEST),

    // Google OAuth Error Codes
    GOOGLE_OAUTH_FAILED(2001, "Google OAuth authentication failed", HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_EXCHANGE_FAILED(
            2002, "Failed to exchange Google authorization code for access token", HttpStatus.BAD_REQUEST),
    GOOGLE_USER_INFO_FAILED(2003, "Failed to retrieve user information from Google", HttpStatus.BAD_REQUEST),
    GOOGLE_USER_CREATION_FAILED(2004, "Failed to create Google user", HttpStatus.INTERNAL_SERVER_ERROR),

    // Password Setup Error Codes
    PASSWORD_SETUP_INVALID(3001, "Invalid password setup request", HttpStatus.BAD_REQUEST),
    PASSWORD_SETUP_NOT_GOOGLE_USER(3002, "User is not a Google user", HttpStatus.BAD_REQUEST),
    PASSWORD_SETUP_ALREADY_SET(3003, "Password already set for this user", HttpStatus.BAD_REQUEST),

    // Profile Completion Error Codes
    PROFILE_COMPLETION_INVALID(4001, "Invalid profile completion request", HttpStatus.BAD_REQUEST),
    PROFILE_COMPLETION_ALREADY_COMPLETE(4002, "Profile already completed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXISTED(1009, "Role not existed", HttpStatus.NOT_FOUND),
    INVALID_TOKEN(1010, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1011, "Token expired", HttpStatus.UNAUTHORIZED),
    INVALID_OTP(1012, "Invalid or expired OTP", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(1013, "Email not verified", HttpStatus.BAD_REQUEST),
    USER_INACTIVE(1014, "User account is inactive", HttpStatus.FORBIDDEN),
    TEACHER_PENDING_APPROVAL(1017, "Teacher account is pending admin approval", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_VERIFIED(1015, "Email already verified", HttpStatus.BAD_REQUEST),
    PASSWORD_CHANGE_REQUIRED(1016, "Password change required", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
