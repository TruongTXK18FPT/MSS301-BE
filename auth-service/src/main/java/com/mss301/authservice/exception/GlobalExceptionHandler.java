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

        // Use specific error codes for common authentication errors
        if ("Unauthenticated".equals(exception.getMessage())) {
            log.info("Handling Unauthenticated exception - returning localized message");
            apiResponse.setCode(ErrorCode.UNAUTHENTICATED.getCode());
            apiResponse.setMessage("Email hoac mat khau khong dung");
        } else if ("User is not active".equals(exception.getMessage())) {
            log.info("Handling User is not active exception");
            apiResponse.setCode(ErrorCode.USER_INACTIVE.getCode());
            apiResponse.setMessage(ErrorCode.USER_INACTIVE.getMessage());
        } else {
            // Fallback to uncategorized for other runtime exceptions
            log.info("Handling other RuntimeException: {}", exception.getMessage());
            apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
            apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
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
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error code: {}", enumKey);
        }

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }
}
