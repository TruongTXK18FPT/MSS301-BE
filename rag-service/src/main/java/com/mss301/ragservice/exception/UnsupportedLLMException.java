package com.mss301.ragservice.exception;

public class UnsupportedLLMException extends RuntimeException {
    public UnsupportedLLMException(String message) {
        super(message);
    }

    public UnsupportedLLMException(String message, Throwable cause) {
        super(message, cause);
    }
}