package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.ExerciseRequest;
import com.mss301.mindmapservice.dto.response.ExerciseResponse;
import com.mss301.mindmapservice.service.ExerciseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/mindmap/exercises")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exercise Management", description = "APIs for managing exercises in mindmap nodes")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    @Operation(summary = "Create a new exercise", description = "Create a new exercise for a mindmap node")
    public ResponseEntity<ApiResponse<ExerciseResponse>> createExercise(
            @Valid @RequestBody ExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = Long.parseLong(jwt.getClaim("userId"));
        ExerciseResponse response = exerciseService.createExercise(request, userId);
        
        return ResponseEntity.ok(ApiResponse.<ExerciseResponse>builder()
                .code("200")
                .message("Exercise created successfully")
                .result(response)
                .build());
    }

    @PutMapping("/{exerciseId}")
    @Operation(summary = "Update an exercise", description = "Update an existing exercise")
    public ResponseEntity<ApiResponse<ExerciseResponse>> updateExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = Long.parseLong(jwt.getClaim("userId"));
        ExerciseResponse response = exerciseService.updateExercise(exerciseId, request, userId);
        
        return ResponseEntity.ok(ApiResponse.<ExerciseResponse>builder()
                .code("200")
                .message("Exercise updated successfully")
                .result(response)
                .build());
    }

    @DeleteMapping("/{exerciseId}")
    @Operation(summary = "Delete an exercise", description = "Delete an exercise by ID")
    public ResponseEntity<ApiResponse<Void>> deleteExercise(
            @PathVariable Long exerciseId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = Long.parseLong(jwt.getClaim("userId"));
        exerciseService.deleteExercise(exerciseId, userId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code("200")
                .message("Exercise deleted successfully")
                .build());
    }

    @GetMapping("/node/{nodeId}")
    @Operation(summary = "Get exercises by node", description = "Get all exercises for a specific node")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> getExercisesByNode(
            @PathVariable Long nodeId) {
        
        List<ExerciseResponse> responses = exerciseService.getExercisesByNode(nodeId);
        
        return ResponseEntity.ok(ApiResponse.<List<ExerciseResponse>>builder()
                .code("200")
                .message("Exercises retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/node/{nodeId}/active")
    @Operation(summary = "Get active exercises", description = "Get all active exercises for a node")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> getActiveExercises(
            @PathVariable Long nodeId) {
        
        List<ExerciseResponse> responses = exerciseService.getActiveExercisesByNode(nodeId);
        
        return ResponseEntity.ok(ApiResponse.<List<ExerciseResponse>>builder()
                .code("200")
                .message("Active exercises retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/node/{nodeId}/difficulty/{difficulty}")
    @Operation(summary = "Get exercises by difficulty", description = "Get exercises filtered by difficulty level")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> getExercisesByDifficulty(
            @PathVariable Long nodeId,
            @PathVariable String difficulty) {
        
        List<ExerciseResponse> responses = exerciseService.getExercisesByDifficulty(nodeId, difficulty);
        
        return ResponseEntity.ok(ApiResponse.<List<ExerciseResponse>>builder()
                .code("200")
                .message("Exercises retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/{exerciseId}")
    @Operation(summary = "Get exercise by ID", description = "Get a specific exercise by its ID")
    public ResponseEntity<ApiResponse<ExerciseResponse>> getExerciseById(
            @PathVariable Long exerciseId) {
        
        ExerciseResponse response = exerciseService.getExerciseById(exerciseId);
        
        return ResponseEntity.ok(ApiResponse.<ExerciseResponse>builder()
                .code("200")
                .message("Exercise retrieved successfully")
                .result(response)
                .build());
    }
}
