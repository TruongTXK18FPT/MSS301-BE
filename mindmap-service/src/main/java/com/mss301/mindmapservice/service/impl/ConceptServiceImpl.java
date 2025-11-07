package com.mss301.mindmapservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.ConceptRequest;
import com.mss301.mindmapservice.dto.request.GenerateConceptRequest;
import com.mss301.mindmapservice.dto.response.ConceptResponse;
import com.mss301.mindmapservice.entity.Concept;
import com.mss301.mindmapservice.repository.ConceptRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.service.AiService;
import com.mss301.mindmapservice.service.ConceptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConceptServiceImpl implements ConceptService {

    private final ConceptRepository conceptRepository;
    private final MindmapNodeRepository mindmapNodeRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public ConceptResponse createConcept(ConceptRequest request) {
        log.info("Creating concept for node: {}", request.getNodeId());

        if (!mindmapNodeRepository.existsById(request.getNodeId())) {
            throw new RuntimeException("Node not found with id: " + request.getNodeId());
        }

        Concept concept = new Concept();
        concept.setNodeId(request.getNodeId());
        concept.setName(request.getName());
        concept.setDefinition(request.getDefinition());
        concept.setExplanation(request.getExplanation());
        concept.setKeyPoints(request.getKeyPoints());
        concept.setExamples(request.getExamples());
        concept.setCommonMistakes(request.getCommonMistakes());
        concept.setTips(request.getTips());
        concept.setPrerequisites(request.getPrerequisites());
        concept.setRelatedConcepts(request.getRelatedConcepts());
        concept.setOrderIndex(request.getOrderIndex());

        Concept saved = conceptRepository.save(concept);
        log.info("Concept created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ConceptResponse updateConcept(Long conceptId, ConceptRequest request) {
        log.info("Updating concept: {}", conceptId);

        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found with id: " + conceptId));

        concept.setName(request.getName());
        concept.setDefinition(request.getDefinition());
        concept.setExplanation(request.getExplanation());
        concept.setKeyPoints(request.getKeyPoints());
        concept.setExamples(request.getExamples());
        concept.setCommonMistakes(request.getCommonMistakes());
        concept.setTips(request.getTips());
        concept.setPrerequisites(request.getPrerequisites());
        concept.setRelatedConcepts(request.getRelatedConcepts());
        concept.setOrderIndex(request.getOrderIndex());

        Concept updated = conceptRepository.save(concept);
        log.info("Concept updated: {}", conceptId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteConcept(Long conceptId) {
        log.info("Deleting concept: {}", conceptId);

        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found with id: " + conceptId));

        conceptRepository.delete(concept);
        log.info("Concept deleted: {}", conceptId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptResponse> getConceptsByNode(Long nodeId) {
        log.info("Getting concepts for node: {}", nodeId);
        return conceptRepository.findByNodeIdOrderByOrderIndexAsc(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConceptResponse getConceptById(Long conceptId) {
        log.info("Getting concept: {}", conceptId);
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found with id: " + conceptId));
        return mapToResponse(concept);
    }

    @Override
    @Transactional
    public List<ConceptResponse> generateConcepts(GenerateConceptRequest request, Long userId) {
        log.info("Generating {} concepts for node {} using AI", request.getNumberOfConcepts(), request.getNodeId());

        try {
            // Call AI service to generate concepts using Gemini
            List<Concept> concepts = aiService.generateConceptsForNode(
                    request.getNodeId(),
                    request.getTopic(),
                    request.getNumberOfConcepts(),
                    userId
            );

            log.info("Successfully generated {} concepts using Gemini AI", concepts.size());

            return concepts.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate concepts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate concepts: " + e.getMessage());
        }
    }

    private ConceptResponse mapToResponse(Concept concept) {
        return ConceptResponse.builder()
                .id(concept.getId())
                .nodeId(concept.getNodeId())
                .name(concept.getName())
                .definition(concept.getDefinition())
                .explanation(concept.getExplanation())
                .keyPoints(concept.getKeyPoints())
                .examples(concept.getExamples())
                .commonMistakes(concept.getCommonMistakes())
                .tips(concept.getTips())
                .prerequisites(concept.getPrerequisites())
                .relatedConcepts(concept.getRelatedConcepts())
                .orderIndex(concept.getOrderIndex())
                .createdAt(concept.getCreatedAt())
                .updatedAt(concept.getUpdatedAt())
                .build();
    }
}
