package com.mss301.chatbotservice.exception;

public class ChatbotServiceException extends RuntimeException {
    public ChatbotServiceException(String message) {
        super(message);
    }

    public ChatbotServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
