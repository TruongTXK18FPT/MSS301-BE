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
public class MindmapContentLinkResponse {
    private Long id;
    private Long mindmapId;
    private Long contentId;
    private String contentType;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
