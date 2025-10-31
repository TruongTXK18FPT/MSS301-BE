package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateExerciseRequest {
    
    @NotNull(message = "Node ID is required")
    private Long nodeId;
    
    @NotBlank(message = "Topic is required")
    private String topic;
    
    @NotBlank(message = "Difficulty is required")
    private String difficulty; // EASY, MEDIUM, HARD, VERY_HARD
    
    @NotBlank(message = "Cognitive level is required")
    private String cognitiveLevel; // RECOGNITION, COMPREHENSION, APPLICATION, ADVANCED_APPLICATION
    
    @Min(value = 1, message = "Number of exercises must be at least 1")
    private Integer numberOfExercises = 3;
}
