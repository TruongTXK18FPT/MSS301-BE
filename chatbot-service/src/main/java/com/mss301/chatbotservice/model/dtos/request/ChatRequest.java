package com.mss301.chatbotservice.model.dtos.request;

import com.mss301.chatbotservice.enums.LLMProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Long sessionId;
    private String content;
    private LLMProvider provider;
}