package com.mss301.chatbotservice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionRequest {
    private Long userId;
    private Long expertProfileId;
    private String expertProfileCode; // Alternative to expertProfileId
}

