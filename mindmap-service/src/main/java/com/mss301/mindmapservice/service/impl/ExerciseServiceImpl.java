package com.mss301.mindmapservice.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.mindmapservice.client.RagServiceClient;
import com.mss301.mindmapservice.dto.rag.RagRequest;
import com.mss301.mindmapservice.dto.rag.RagResponse;
import com.mss301.mindmapservice.dto.request.ExerciseRequest;
import com.mss301.mindmapservice.dto.request.GenerateExerciseRequest;
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
    private final RagServiceClient ragServiceClient;

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

    @Override
    @Transactional
    public List<ExerciseResponse> generateExercises(GenerateExerciseRequest request, Long userId) {
        log.info("Generating {} exercises for node: {} using AI", request.getNumberOfExercises(), request.getNodeId());

        // Verify node exists
        if (!mindmapNodeRepository.existsById(request.getNodeId())) {
            throw new RuntimeException("Node not found with id: " + request.getNodeId());
        }

        try {
            // Call RAG service to generate exercises
            String exercisesJson = callRagServiceForExerciseGeneration(request);
            
            // Parse the AI response and create exercises
            List<Exercise> exercises = parseExercisesFromAiResponse(request.getNodeId(), exercisesJson, userId);
            
            // Save all exercises
            List<Exercise> savedExercises = exerciseRepository.saveAll(exercises);
            log.info("Successfully generated and saved {} exercises", savedExercises.size());
            
            return savedExercises.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to generate exercises: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate exercises: " + e.getMessage());
        }
    }

    private String callRagServiceForExerciseGeneration(GenerateExerciseRequest request) {
        log.info("Calling RAG service for exercise generation");

        // Build the query for exercise generation
        String queryText = String.format(
            "Generate %d mathematics exercises about '%s' with difficulty level '%s' and cognitive level '%s'. " +
            "Each exercise should be appropriate for the specified difficulty and cognitive level.",
            request.getNumberOfExercises(),
            request.getTopic(),
            request.getDifficulty(),
            request.getCognitiveLevel()
        );

        RagRequest ragRequest = RagRequest.builder()
                .queryText(queryText)
                .mode("EXERCISE")
                .llmProvider("MISTRAL")
                .useDocuments(false)
                .build();

        try {
            RagResponse ragResponse = ragServiceClient.processRagQuery(ragRequest);
            
            if (ragResponse == null || ragResponse.getContent() == null) {
                throw new RuntimeException("RAG service returned null or invalid response");
            }

            // Extract exercise content from response
            Object contentObj = ragResponse.getContent();
            if (contentObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) contentObj;
                Object exercisesContent = contentMap.get("exercisesContent");
                if (exercisesContent != null) {
                    return exercisesContent.toString();
                }
            }
            
            throw new RuntimeException("Failed to extract exercises content from RAG response");
            
        } catch (Exception e) {
            log.error("Error calling RAG service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call RAG service: " + e.getMessage());
        }
    }

    private List<Exercise> parseExercisesFromAiResponse(Long nodeId, String exercisesJson, Long userId) {
        log.info("Parsing exercises from AI response");
        
        List<Exercise> exercises = new ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(exercisesJson);
            
            JsonNode exercisesArray = root.path("exercises");
            if (!exercisesArray.isArray()) {
                throw new RuntimeException("Expected 'exercises' array in AI response");
            }

            int orderIndex = 1;
            for (JsonNode exerciseNode : exercisesArray) {
                Exercise exercise = new Exercise();
                exercise.setNodeId(nodeId);
                exercise.setQuestion(exerciseNode.path("question").asText());
                exercise.setAnswer(exerciseNode.path("answer").asText());
                exercise.setSolution(exerciseNode.path("solution").asText());
                
                // Parse difficulty
                String difficulty = exerciseNode.path("difficulty").asText("MEDIUM");
                exercise.setDifficulty(Exercise.DifficultyLevel.valueOf(difficulty.toUpperCase()));
                
                // Parse cognitive level
                String cognitiveLevel = exerciseNode.path("cognitiveLevel").asText("COMPREHENSION");
                exercise.setCognitiveLevel(Exercise.CognitiveLevel.valueOf(cognitiveLevel.toUpperCase()));
                
                // Optional fields
                if (exerciseNode.has("estimatedTime")) {
                    exercise.setEstimatedTime(exerciseNode.path("estimatedTime").asInt());
                }
                if (exerciseNode.has("hints")) {
                    exercise.setHints(exerciseNode.path("hints").asText());
                }
                
                exercise.setOrderIndex(orderIndex++);
                exercise.setIsActive(true);
                exercise.setCreatedBy(userId);
                
                exercises.add(exercise);
            }
            
            log.info("Parsed {} exercises from AI response", exercises.size());
            return exercises;
            
        } catch (Exception e) {
            log.error("Error parsing exercises JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse exercises from AI response: " + e.getMessage());
        }
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
