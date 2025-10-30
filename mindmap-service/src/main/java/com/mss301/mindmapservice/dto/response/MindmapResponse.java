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
public class MindmapResponse {

    private Long id;
    private String title;
    private String description;
    private Long userId;
    private String grade;
    private String subject;
    private Boolean isPublic;
    private Boolean isAiGenerated;
    private String aiProvider;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessedAt;
    private Integer accessCount;
    private Integer favoriteCount;
    private Integer shareCount;
    private String color;
    private String difficulty;
    private String cognitiveLevel;
    private String estimatedTime;
    private String thumbnailUrl;
    private String tags;
    private List<MindmapNodeResponse> nodes;
    private List<MindmapEdgeResponse> edges;
}
