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
public class FormulaRequest {

    @NotNull(message = "Node ID is required")
    private Long nodeId;

    @NotBlank(message = "Formula name is required")
    private String name;

    @NotBlank(message = "Formula text is required")
    private String formulaText;

    private String formulaLatex;
    private String description;
    private String usageExample;
    private String variables; // JSON
    private String conditions;

    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private Boolean isPrimary = false;
}
