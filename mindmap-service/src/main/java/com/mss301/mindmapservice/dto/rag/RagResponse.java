package com.mss301.mindmapservice.dto.rag;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {

    private String mode;
    private String llmProvider;
    private String queryText;
    private Object response;
    private LocalDateTime timestamp;
    private Integer totalResults;
    private List<RagResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagResult {
        private String content;
        private String documentId;
        private String chunkId;
        private Double score;
        private String source;
    }
}
