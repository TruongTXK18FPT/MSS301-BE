package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.ExerciseRequest;
import com.mss301.mindmapservice.dto.request.GenerateExerciseRequest;
import com.mss301.mindmapservice.dto.response.ExerciseResponse;

public interface ExerciseService {

    /**
     * Create a new exercise for a node
     */
    ExerciseResponse createExercise(ExerciseRequest request, Long userId);

    /**
     * Update an existing exercise
     */
    ExerciseResponse updateExercise(Long exerciseId, ExerciseRequest request, Long userId);

    /**
     * Delete an exercise
     */
    void deleteExercise(Long exerciseId, Long userId);

    /**
     * Get all exercises for a node
     */
    List<ExerciseResponse> getExercisesByNode(Long nodeId);

    /**
     * Get active exercises for a node
     */
    List<ExerciseResponse> getActiveExercisesByNode(Long nodeId);

    /**
     * Get exercises by difficulty
     */
    List<ExerciseResponse> getExercisesByDifficulty(Long nodeId, String difficulty);

    /**
     * Get exercises by cognitive level
     */
    List<ExerciseResponse> getExercisesByCognitiveLevel(Long nodeId, String cognitiveLevel);

    /**
     * Get a single exercise by ID
     */
    ExerciseResponse getExerciseById(Long exerciseId);

    /**
     * Generate exercises using AI
     */
    List<ExerciseResponse> generateExercises(GenerateExerciseRequest request, Long userId);
}
