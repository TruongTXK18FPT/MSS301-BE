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
public class FormulaResponse {

    private Long id;
    private Long nodeId;
    private String name;
    private String formulaText;
    private String formulaLatex;
    private String description;
    private String usageExample;
    private String variables;
    private String conditions;
    private Integer orderIndex;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
