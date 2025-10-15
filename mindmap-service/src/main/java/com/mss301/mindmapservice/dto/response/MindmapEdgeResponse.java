package com.mss301.mindmapservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.mindmapservice.entity.MindmapEdge.RelationshipType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapEdgeResponse {

    private Long id;
    private Long mindmapId;
    private Long fromNodeId;
    private Long toNodeId;
    private RelationshipType relationshipType;
    private String label;
    private String color;
    private Integer thickness;
    private String style;
    private Boolean isDirected;
    private Double weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
