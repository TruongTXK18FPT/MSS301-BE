package com.mss301.mindmapservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.mindmapservice.entity.MindmapNode.NodeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapNodeResponse {

    private Long id;
    private Long mindmapId;
    private String title;
    private String content;
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
    private Boolean isBold;
    private Boolean isItalic;
    private Boolean isUnderline;
    private Long parentNodeId;
    private Integer level;
    private Integer orderIndex;
    private Boolean isCollapsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
