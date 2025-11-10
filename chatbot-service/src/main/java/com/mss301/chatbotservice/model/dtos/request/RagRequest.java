package com.mss301.chatbotservice.model.dtos.request;


import com.mss301.chatbotservice.enums.LLMProvider;
import com.mss301.chatbotservice.enums.ResponseMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagRequest {
    private String documentId = "";
    private String chapterId = "";
    private String lessonId = "";
    private String queryText;
    private ResponseMode mode = ResponseMode.CHAT;
    private LLMProvider llmProvider = LLMProvider.GEMINI;
    private boolean useSemantic = true;
    private Boolean useDocuments = false; // Control whether to retrieve documents or not
    private Integer topK = 7;
}
