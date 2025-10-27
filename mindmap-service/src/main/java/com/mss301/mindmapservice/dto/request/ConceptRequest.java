package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptRequest {

    @NotNull(message = "Node ID is required")
    private Long nodeId;

    @NotBlank(message = "Concept name is required")
    private String name;

    @NotBlank(message = "Definition is required")
    private String definition;

    private String explanation;
    private String keyPoints; // JSON
    private String examples; // JSON
    private String commonMistakes; // JSON
    private String tips;
    private String prerequisites;
    private String relatedConcepts;

    @Builder.Default
    private Integer orderIndex = 0;
}
