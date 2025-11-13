package com.mss301.contentservice.dto.response;

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
public class ContentItemResponse {
    private Long id;
    private Long ownerId;
    private String type;
    private String title;
    private String description;
    private String content;
    private String subject;
    private String grade;
    private List<String> tags; // Changed from String to List<String>
    private Boolean isPublic;
    private Long classroomId;  // Classroom association
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // type-specific response blocks
    private QuizResponsePayload quiz;
    private AssignmentDetailResponse assignment;
}
