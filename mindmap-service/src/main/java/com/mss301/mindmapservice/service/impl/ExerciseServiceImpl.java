package com.mss301.mindmapservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.ExerciseRequest;
import com.mss301.mindmapservice.dto.response.ExerciseResponse;
import com.mss301.mindmapservice.entity.Exercise;
import com.mss301.mindmapservice.repository.ExerciseRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.service.ExerciseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MindmapNodeRepository mindmapNodeRepository;

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
    public List<ExerciseResponse> getExercisesByNode(Long nodeId) {
        log.info("Getting exercises for node: {}", nodeId);
        return exerciseRepository.findByNodeId(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExerciseResponse> getActiveExercisesByNode(Long nodeId) {
        log.info("Getting active exercises for node: {}", nodeId);
        return exerciseRepository.findByNodeIdAndIsActiveTrue(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExerciseResponse> getExercisesByDifficulty(Long nodeId, String difficulty) {
        log.info("Getting exercises for node: {} with difficulty: {}", nodeId, difficulty);
        Exercise.DifficultyLevel level = Exercise.DifficultyLevel.valueOf(difficulty.toUpperCase());
        return exerciseRepository.findByNodeIdAndDifficulty(nodeId, level).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExerciseResponse> getExercisesByCognitiveLevel(Long nodeId, String cognitiveLevel) {
        log.info("Getting exercises for node: {} with cognitive level: {}", nodeId, cognitiveLevel);
        Exercise.CognitiveLevel level = Exercise.CognitiveLevel.valueOf(cognitiveLevel.toUpperCase());
        return exerciseRepository.findByNodeIdAndCognitiveLevel(nodeId, level).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExerciseResponse getExerciseById(Long exerciseId) {
        log.info("Getting exercise: {}", exerciseId);
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found with id: " + exerciseId));
        return mapToResponse(exercise);
    }

    private ExerciseResponse mapToResponse(Exercise exercise) {
        return ExerciseResponse.builder()
                .id(exercise.getId())
                .nodeId(exercise.getNodeId())
                .question(exercise.getQuestion())
                .answer(exercise.getAnswer())
                .solution(exercise.getSolution())
                .difficulty(exercise.getDifficulty())
                .cognitiveLevel(exercise.getCognitiveLevel())
                .estimatedTime(exercise.getEstimatedTime())
                .hints(exercise.getHints())
                .orderIndex(exercise.getOrderIndex())
                .isActive(exercise.getIsActive())
                .createdBy(exercise.getCreatedBy())
                .createdAt(exercise.getCreatedAt())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
