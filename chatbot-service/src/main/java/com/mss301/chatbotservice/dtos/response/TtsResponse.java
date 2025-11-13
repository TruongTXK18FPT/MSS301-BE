package com.mss301.chatbotservice.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {
    private boolean success;
    private String message;
    private String audioUrl;
}

