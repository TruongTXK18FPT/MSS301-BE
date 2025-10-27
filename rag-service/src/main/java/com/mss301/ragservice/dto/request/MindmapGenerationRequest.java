package com.mss301.ragservice.dto.request;

import com.mss301.ragservice.enums.LLMProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapGenerationRequest {
    private String topic;
    private Integer grade;
    private String subject;
    private String description;
    private String additionalContext;
    
    private LLMProvider llmProvider;
    private String aiModel;
    
    @Builder.Default
    private Integer minNodes = 10;
    
    @Builder.Default
    private Integer maxNodes = 20;
    
    @Builder.Default
    private Integer maxDepth = 3;
    
    @Builder.Default
    private Boolean includeExamples = true;
    
    @Builder.Default
    private Boolean includeExercises = true;
    
    @Builder.Default
    private Boolean includeFormulas = true;
    
    @Builder.Default
    private Boolean includeConcepts = true;
    
    // RAG-specific
    private String documentQuery;
    
    @Builder.Default
    private Boolean useRag = true;
    
    @Builder.Default
    private Integer maxDocuments = 5;
}
