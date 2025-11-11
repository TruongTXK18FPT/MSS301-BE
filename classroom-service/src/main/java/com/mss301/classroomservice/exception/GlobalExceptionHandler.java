package com.mss301.classroomservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.mss301.classroomservice.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        StringBuilder errorMsg = new StringBuilder("Validation failed: ");
        errors.forEach((k, v) -> errorMsg.append(k).append("=").append(v).append("; "));

        ApiResponse<?> response = ApiResponse.error("400", errorMsg.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        ex.printStackTrace(); // Log the full stack trace
        System.err.println("ERROR: " + ex.getMessage());

        ApiResponse<?> response = ApiResponse.error("500", ex.getMessage() != null ? ex.getMessage() : "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex, WebRequest request) {
        ex.printStackTrace(); // Log the full stack trace
        System.err.println("EXCEPTION: " + ex.getClass().getName() + " - " + ex.getMessage());

        String errorMsg = "An error occurred: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        ApiResponse<?> response = ApiResponse.error("500", errorMsg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
