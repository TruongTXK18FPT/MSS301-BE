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
public class ClassroomMindmapResponse {

    private Long id;
    private Long mindmapId;
    private Long classroomId;
    private Long teacherId;
    private Boolean isActive;
    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    
    // Thông tin mindmap
    private String mindmapTitle;
    private String mindmapDescription;
}
