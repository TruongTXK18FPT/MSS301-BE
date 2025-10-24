package com.mss301.mindmapservice.dto.response;

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
public class RagMindmapResponse {

    private Long mindmapId;
    private String title;
    private String description;
    private String aiProvider;
    private String aiModel;
    private String status;
    private String errorMessage;

    // Mindmap generation results
    private Integer nodesGenerated;
    private Integer edgesGenerated;

    // RAG-specific results
    private Integer documentsUsed;
    private List<String> sourceDocuments;
    private Double averageRelevanceScore;
    private String ragContext;

    // Metadata
    private LocalDateTime createdAt;
    private Long processingTimeMs;
}
