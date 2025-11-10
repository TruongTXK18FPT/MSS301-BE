package com.mss301.chatbotservice.model.dtos.response;

import com.mss301.chatbotservice.model.ChatMessage;
import com.mss301.chatbotservice.model.ExpertProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionReponse {
    private Long id;
    private Long userId;
    private ExpertProfile expertProfileId;
    private String title;
    private Boolean status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
