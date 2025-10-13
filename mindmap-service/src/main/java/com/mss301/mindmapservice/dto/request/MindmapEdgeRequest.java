package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.NotNull;

import com.mss301.mindmapservice.entity.MindmapEdge.RelationshipType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapEdgeRequest {

    @NotNull(message = "From node ID is required")
    private Long fromNodeId;

    @NotNull(message = "To node ID is required")
    private Long toNodeId;

    private RelationshipType relationshipType;
    private String label;
    private String color;
    private Integer thickness = 2;
    private String style;
    private Boolean isDirected = true;
    private Double weight = 1.0;
}
