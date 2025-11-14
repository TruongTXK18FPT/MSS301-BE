package com.mss301.chatbotservice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    private String messages;
    private String gradeLevel; // Cho phép chọn lớp trước khi chat
    private String documentId; // ID của document để chat theo giáo trình (RAG)
    private String chapterId; // ID của chapter (RAG)
    private String lessonId; // ID của lesson (RAG)
    private String fileStoreName; // Google File Search Store name (cho RAG với file-search)
    private Boolean useVoiceChat = false; // Sử dụng voice chat với TTS
}

