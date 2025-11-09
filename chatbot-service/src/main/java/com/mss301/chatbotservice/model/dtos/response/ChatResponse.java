package com.mss301.chatbotservice.model.dtos.response;

import com.mss301.chatbotservice.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long messageId;
    private ChatRole role;
    private String content;
    private Long tokensUsed;
    private LocalDateTime createdAt;
}