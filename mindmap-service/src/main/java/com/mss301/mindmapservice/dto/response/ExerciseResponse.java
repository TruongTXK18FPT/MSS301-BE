package com.mss301.mindmapservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.mindmapservice.entity.Exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponse {

    private Long id;
    private Long nodeId;
    private String question;
    private String answer;
    private String solution;
    private Exercise.DifficultyLevel difficulty;
    private Exercise.CognitiveLevel cognitiveLevel;
    private Integer estimatedTime;
    private String hints;
    private Integer orderIndex;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
