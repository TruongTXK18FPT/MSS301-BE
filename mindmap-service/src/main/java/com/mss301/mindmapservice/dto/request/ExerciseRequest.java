package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.mss301.mindmapservice.entity.Exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRequest {

    @NotNull(message = "Node ID is required")
    private Long nodeId;

    @NotBlank(message = "Question is required")
    private String question;

    private String answer;
    private String solution;

    @Builder.Default
    private Exercise.DifficultyLevel difficulty = Exercise.DifficultyLevel.MEDIUM;

    @Builder.Default
    private Exercise.CognitiveLevel cognitiveLevel = Exercise.CognitiveLevel.COMPREHENSION;

    private Integer estimatedTime; // in minutes

    private String hints; // JSON array

    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private Boolean isActive = true;
}
