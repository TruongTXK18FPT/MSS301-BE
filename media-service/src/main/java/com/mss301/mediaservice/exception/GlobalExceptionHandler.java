package com.mss301.mediaservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("File size exceeded: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "File size too large");
        error.put("message", "Maximum file size is 10MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandlerFound(NoHandlerFoundException e) {
        // Don't log actuator health check 404s as errors
        if (e.getRequestURL() != null && e.getRequestURL().contains("/actuator/")) {
            return null; // Let Spring Boot handle actuator endpoints
        }
        log.error("No handler found: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Not found");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        // Don't catch actuator-related exceptions
        if (e.getMessage() != null && e.getMessage().contains("actuator")) {
            log.debug("Actuator-related exception, ignoring: {}", e.getMessage());
            return null; // Let Spring Boot handle it
        }

        log.error("Unexpected error: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
