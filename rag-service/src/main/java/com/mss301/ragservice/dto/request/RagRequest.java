package com.mss301.ragservice.dto.request;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.enums.ResponseMode;

import lombok.Data;

@Data
public class RagRequest {
    private String documentId;
    private String chapterId;
    private String lessonId;
    private String fileStoreName; // Google File Search Store name (cho file-search)
    private String queryText;
    private ResponseMode mode;
    private LLMProvider llmProvider;
    private boolean useSemantic = true;
    private Boolean useDocuments = false; // Control whether to retrieve documents or not
    private Integer topK = 7;
}
