package com.mss301.mindmapservice.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.mindmapservice.dto.request.ExerciseRequest;
import com.mss301.mindmapservice.dto.request.GenerateExerciseRequest;
import com.mss301.mindmapservice.dto.response.ExerciseResponse;
import com.mss301.mindmapservice.entity.Exercise;
import com.mss301.mindmapservice.repository.ExerciseRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.service.AiService;
import com.mss301.mindmapservice.service.ExerciseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MindmapNodeRepository mindmapNodeRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public ExerciseResponse createExercise(ExerciseRequest request, Long userId) {
        log.info("Creating exercise for node: {} by user: {}", request.getNodeId(), userId);

        // Verify node exists
        if (!mindmapNodeRepository.existsById(request.getNodeId())) {
            throw new RuntimeException("Node not found with id: " + request.getNodeId());
        }

        Exercise exercise = new Exercise();
        exercise.setNodeId(request.getNodeId());
        exercise.setQuestion(request.getQuestion());
        exercise.setAnswer(request.getAnswer());
        exercise.setSolution(request.getSolution());
        exercise.setDifficulty(request.getDifficulty());
        exercise.setCognitiveLevel(request.getCognitiveLevel());
        exercise.setEstimatedTime(request.getEstimatedTime());
        exercise.setHints(request.getHints());
        exercise.setOrderIndex(request.getOrderIndex());
        exercise.setIsActive(request.getIsActive());
        exercise.setCreatedBy(userId);

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Exercise created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExerciseResponse updateExercise(Long exerciseId, ExerciseRequest request, Long userId) {
        log.info("Updating exercise: {} by user: {}", exerciseId, userId);

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found with id: " + exerciseId));

        exercise.setQuestion(request.getQuestion());
        exercise.setAnswer(request.getAnswer());
        exercise.setSolution(request.getSolution());
        exercise.setDifficulty(request.getDifficulty());
        exercise.setCognitiveLevel(request.getCognitiveLevel());
        exercise.setEstimatedTime(request.getEstimatedTime());
        exercise.setHints(request.getHints());
        exercise.setOrderIndex(request.getOrderIndex());
        exercise.setIsActive(request.getIsActive());

        Exercise updated = exerciseRepository.save(exercise);
        log.info("Exercise updated: {}", exerciseId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteExercise(Long exerciseId, Long userId) {
        log.info("Deleting exercise: {} by user: {}", exerciseId, userId);

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found with id: " + exerciseId));

        exerciseRepository.delete(exercise);
        log.info("Exercise deleted: {}", exerciseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getExercisesByNode(Long nodeId) {
        log.info("Getting exercises for node: {}", nodeId);
        return exerciseRepository.findByNodeId(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getActiveExercisesByNode(Long nodeId) {
        log.info("Getting active exercises for node: {}", nodeId);
        return exerciseRepository.findByNodeIdAndIsActiveTrue(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getExercisesByDifficulty(Long nodeId, String difficulty) {
        log.info("Getting exercises for node: {} with difficulty: {}", nodeId, difficulty);
        Exercise.DifficultyLevel level = Exercise.DifficultyLevel.valueOf(difficulty.toUpperCase());
        return exerciseRepository.findByNodeIdAndDifficulty(nodeId, level).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getExercisesByCognitiveLevel(Long nodeId, String cognitiveLevel) {
        log.info("Getting exercises for node: {} with cognitive level: {}", nodeId, cognitiveLevel);
        Exercise.CognitiveLevel level = Exercise.CognitiveLevel.valueOf(cognitiveLevel.toUpperCase());
        return exerciseRepository.findByNodeIdAndCognitiveLevel(nodeId, level).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseResponse getExerciseById(Long exerciseId) {
        log.info("Getting exercise: {}", exerciseId);
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found with id: " + exerciseId));
        return mapToResponse(exercise);
    }

    @Override
    @Transactional
    public List<ExerciseResponse> generateExercises(GenerateExerciseRequest request, Long userId) {
        log.info("Generating {} exercises for node: {} using AI", request.getNumberOfExercises(), request.getNodeId());

        // Verify node exists
        if (!mindmapNodeRepository.existsById(request.getNodeId())) {
            throw new RuntimeException("Node not found with id: " + request.getNodeId());
        }

        try {
            // Call AI service to generate exercises using Gemini (replaces RAG service)
            List<Exercise> exercises = aiService.generateExercisesForNode(
                    request.getNodeId(),
                    request.getTopic(),
                    request.getDifficulty(),
                    request.getCognitiveLevel(),
                    request.getNumberOfExercises(),
                    userId
            );

            log.info("Successfully generated {} exercises using Gemini AI", exercises.size());

            return exercises.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate exercises: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate exercises: " + e.getMessage());
        }
    }

    private ExerciseResponse mapToResponse(Exercise exercise) {
        List<String> parsedHints = parseHints(exercise.getHints());

        return ExerciseResponse.builder()
                .id(exercise.getId())
                .nodeId(exercise.getNodeId())
                .question(exercise.getQuestion())
                .answer(exercise.getAnswer())
                .solution(exercise.getSolution())
                .difficulty(exercise.getDifficulty())
                .cognitiveLevel(exercise.getCognitiveLevel())
                .estimatedTime(exercise.getEstimatedTime())
                .hints(parsedHints)
                .orderIndex(exercise.getOrderIndex())
                .isActive(exercise.getIsActive())
                .createdBy(exercise.getCreatedBy())
                .createdAt(exercise.getCreatedAt())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }

    private List<String> parseHints(String hintsJson) {
        if (hintsJson == null || hintsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Try to parse as JSON array
            ObjectMapper mapper = new ObjectMapper();
            JsonNode hintsNode = mapper.readTree(hintsJson);

            if (hintsNode.isArray()) {
                List<String> hints = new ArrayList<>();
                for (JsonNode hint : hintsNode) {
                    hints.add(hint.asText());
                }
                return hints;
            } else if (hintsNode.isTextual()) {
                // If it's a single string, return as single-item list
                return Arrays.asList(hintsNode.asText());
            }
        } catch (Exception e) {
            // Not JSON format - try splitting by newlines
            log.debug("Hints is not JSON, attempting to split by newlines: {}", hintsJson.substring(0, Math.min(100, hintsJson.length())));
            
            // Split by newlines and filter out empty lines
            List<String> hints = Arrays.stream(hintsJson.split("\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            
            if (!hints.isEmpty()) {
                log.debug("Successfully parsed {} hints from newline-separated text", hints.size());
                return hints;
            }
        }

        // Fallback: treat as single string
        log.debug("Using fallback: treating hints as single string");
        return Arrays.asList(hintsJson);
    }
}
