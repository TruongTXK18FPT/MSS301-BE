package com.mss301.chatbotservice.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionResponse {
    private Long id;
    private Long userId;
    private Long expertProfileId;
    private String expertProfileName;
    private String title;
    private Boolean status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

