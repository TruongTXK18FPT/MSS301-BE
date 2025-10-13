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
public class AiGenerateMindmapResponse {

    private Long mindmapId;
    private String title;
    private String description;
    private String aiProvider;
    private String aiModel;
    private String status; // SUCCESS, FAILED, PARTIAL
    private String errorMessage;
    private Integer nodesGenerated;
    private Integer edgesGenerated;
    private LocalDateTime createdAt;
    private MindmapResponse mindmap;
}
