package com.mss301.classroomservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomContentResponse {
    private Long id;
    private Long classroomId;
    private Long contentId;
    private String type;
    private Boolean visible;
    private Integer orderIndex;
    private LocalDateTime publishAt;
    private LocalDateTime dueAt;
    private Integer maxPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
