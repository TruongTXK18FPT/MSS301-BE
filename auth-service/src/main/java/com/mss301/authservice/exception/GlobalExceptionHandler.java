package com.mss301.authservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mss301.authservice.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse<Object>> handlingRuntimeException(RuntimeException exception) {
        log.error("GlobalExceptionHandler caught RuntimeException: {}", exception.getMessage());
        log.error("Exception: ", exception);
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        String message = exception.getMessage();

        // Use specific error codes for common authentication errors
        if ("Unauthenticated".equals(message)) {
            log.info("Handling Unauthenticated exception - returning localized message");
            apiResponse.setCode(ErrorCode.UNAUTHENTICATED.getCode());
            apiResponse.setMessage("Email hoac mat khau khong dung");
        } else if ("User is not active".equals(message)) {
            log.info("Handling User is not active exception");
            apiResponse.setCode(ErrorCode.USER_INACTIVE.getCode());
            apiResponse.setMessage(ErrorCode.USER_INACTIVE.getMessage());
        } else if ("Email không tồn tại trong hệ thống".equals(message)) {
            log.info("Handling Email not found exception");
            apiResponse.setCode(ErrorCode.USER_NOT_EXISTED.getCode());
            apiResponse.setMessage(message);
        } else if ("Mật khẩu không đúng".equals(message)) {
            log.info("Handling Incorrect password exception");
            apiResponse.setCode(ErrorCode.UNAUTHENTICATED.getCode());
            apiResponse.setMessage(message);
        } else if (message != null && message.contains("Email chưa được xác thực")) {
            log.info("Handling Email not verified exception");
            apiResponse.setCode(ErrorCode.EMAIL_NOT_VERIFIED.getCode());
            apiResponse.setMessage(message);
        } else if (message != null && (message.contains("Invalid OTP") || message.contains("OTP expired")
                || message.contains("Mã OTP không đúng"))) {
            log.info("Handling Invalid/Expired OTP exception");
            apiResponse.setCode(ErrorCode.INVALID_OTP.getCode());
            apiResponse.setMessage(message);
        } else {
            // Fallback to uncategorized for other runtime exceptions
            log.info("Handling other RuntimeException: {}", message);
            apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
            // Use exception message if available, otherwise use default
            apiResponse.setMessage(message != null ? message : ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }

        log.info("Returning API response: code={}, message={}", apiResponse.getCode(), apiResponse.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Object>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Object> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handlingValidation(MethodArgumentNotValidException exception) {
        log.error("GlobalExceptionHandler caught MethodArgumentNotValidException: {}", exception.getMessage());

        // Get the first validation error message
        String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage()
                        : error.getField() + " không hợp lệ")
                .orElse("Dữ liệu không hợp lệ");

        log.info("Validation error message: {}", errorMessage);

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setCode(ErrorCode.INVALID_KEY.getCode());
        apiResponse.setMessage(errorMessage);

        return ResponseEntity.badRequest().body(apiResponse);
    }
}
