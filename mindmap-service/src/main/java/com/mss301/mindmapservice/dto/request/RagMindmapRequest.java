package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest.AiProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagMindmapRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotNull(message = "Grade is required")
    @Min(value = 1, message = "Grade must be at least 1")
    @Max(value = 12, message = "Grade must be at most 12")
    private Integer grade;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
    private String additionalContext;

    @NotNull(message = "AI provider is required")
    private AiProvider aiProvider;

    private String aiModel;

    @Min(value = 5, message = "Max nodes must be at least 5")
    @Max(value = 100, message = "Max nodes must be at most 100")
    @Builder.Default
    private Integer maxNodes = 20;

    @Min(value = 1, message = "Max depth must be at least 1")
    @Max(value = 5, message = "Max depth must be at most 5")
    @Builder.Default
    private Integer maxDepth = 3;

    @Builder.Default
    private Boolean includeExamples = true;

    @Builder.Default
    private Boolean includeExercises = true;

    @Builder.Default
    private Boolean includeFormulas = true;

    // RAG-specific fields
    private String documentQuery;

    @Builder.Default
    private Boolean useRag = true;

    @Builder.Default
    private Integer maxDocuments = 5;

    @Builder.Default
    private Double similarityThreshold = 0.7;

    // Document-based mindmap fields
    @Builder.Default
    private Boolean useDocuments = false;
    
    private Long documentId;
    private Long chapterId;
    private Long lessonId;
}
