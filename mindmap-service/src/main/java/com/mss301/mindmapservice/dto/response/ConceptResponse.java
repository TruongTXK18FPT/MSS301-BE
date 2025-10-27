package com.mss301.mindmapservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptResponse {

    private Long id;
    private Long nodeId;
    private String name;
    private String definition;
    private String explanation;
    private String keyPoints;
    private String examples;
    private String commonMistakes;
    private String tips;
    private String prerequisites;
    private String relatedConcepts;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
