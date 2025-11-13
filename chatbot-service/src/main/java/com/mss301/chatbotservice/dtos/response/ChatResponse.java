package com.mss301.chatbotservice.dtos.response;

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
    private String audioUrl; // URL của audio từ TTS (nếu có)
    private java.util.List<Source> sources; // Sources từ RAG (nếu có)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String content;
        private Double score;
        private String documentId;
        private String chapterId;
        private String lessonId;
        private String chapterTitle;
        private String lessonTitle;
        private Long pageNumber;
    }
}

