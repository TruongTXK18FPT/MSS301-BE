package com.mss301.mindmapservice.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 50, message = "Grade must not exceed 50 characters")
    private String grade;

    @Size(max = 100, message = "Subject must not exceed 100 characters")
    private String subject;

    @Builder.Default
    private Boolean isPublic = false;

    // For bulk update of nodes and edges
    private List<MindmapNodeRequest> nodes;
    private List<MindmapEdgeRequest> edges;
}
