package com.mss301.chatbotservice.chatbot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Message {
    private String role;
    private String content;
}