package com.mss301.retrievalservice.dto.request;

import lombok.Data;

@Data
public class RetrievalRequest {
    private String documentId;
    private String chapterId;
    private String lessonId;
    private String queryText;
    private boolean useSemantic;
    private int topK = 7;
}
