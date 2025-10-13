package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mss301.mindmapservice.entity.MindmapNode.NodeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapNodeRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    @NotNull(message = "Node type is required")
    private NodeType nodeType;

    private Double positionX;
    private Double positionY;
    private Double width;
    private Double height;
    private String color;
    private String backgroundColor;
    private String borderColor;
    private Integer fontSize;
    private String fontFamily;
    private Boolean isBold = false;
    private Boolean isItalic = false;
    private Boolean isUnderline = false;
    private Long parentNodeId;
    private Integer level = 0;
    private Integer orderIndex = 0;
    private Boolean isCollapsed = false;
}
