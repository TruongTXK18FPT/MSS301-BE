package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.ConceptRequest;
import com.mss301.mindmapservice.dto.request.GenerateConceptRequest;
import com.mss301.mindmapservice.dto.response.ConceptResponse;

public interface ConceptService {

    /**
     * Create a new concept for a node
     */
    ConceptResponse createConcept(ConceptRequest request);

    /**
     * Update an existing concept
     */
    ConceptResponse updateConcept(Long conceptId, ConceptRequest request);

    /**
     * Delete a concept
     */
    void deleteConcept(Long conceptId);

    /**
     * Get all concepts for a node
     */
    List<ConceptResponse> getConceptsByNode(Long nodeId);

    /**
     * Get a single concept by ID
     */
    ConceptResponse getConceptById(Long conceptId);

    /**
     * Generate concepts using AI
     */
    List<ConceptResponse> generateConcepts(GenerateConceptRequest request, Long userId);
}
