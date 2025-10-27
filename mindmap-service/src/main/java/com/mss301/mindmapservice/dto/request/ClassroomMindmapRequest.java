package com.mss301.mindmapservice.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomMindmapRequest {

    @NotNull(message = "Mindmap ID is required")
    private Long mindmapId;

    @NotNull(message = "Classroom ID is required")
    private Long classroomId;

    private LocalDateTime expiresAt;
}
