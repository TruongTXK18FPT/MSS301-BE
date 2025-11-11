package com.mss301.classroomservice.dto.request;

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
public class ClassroomContentRequest {
    private Long contentId; // Nullable for lessons (embedded content)

    @NotNull
    private String type; // LESSON/ASSIGNMENT/QUIZ/RESOURCE

    // Fields for embedded content (lessons)
    private String title;
    private String description;
    private String content; // Lesson content stored directly

    private Boolean visible = true;
    private Integer orderIndex;
    private LocalDateTime publishAt;
    private LocalDateTime dueAt;
    private Integer maxPoints;
}
