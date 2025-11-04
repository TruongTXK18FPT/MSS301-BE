package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.ConceptRequest;
import com.mss301.mindmapservice.dto.response.ConceptResponse;
import com.mss301.mindmapservice.service.ConceptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/mindmap/concepts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Concept Management", description = "APIs for managing concepts in mindmap nodes")
public class ConceptController {

    private final ConceptService conceptService;

    @PostMapping
    @Operation(summary = "Create a new concept", description = "Create a new concept for a mindmap node")
    public ResponseEntity<ApiResponse<ConceptResponse>> createConcept(
            @Valid @RequestBody ConceptRequest request) {
        
        ConceptResponse response = conceptService.createConcept(request);
        
        return ResponseEntity.ok(ApiResponse.<ConceptResponse>builder()
                .code("200")
                .message("Concept created successfully")
                .result(response)
                .build());
    }

    @PutMapping("/{conceptId}")
    @Operation(summary = "Update a concept", description = "Update an existing concept")
    public ResponseEntity<ApiResponse<ConceptResponse>> updateConcept(
            @PathVariable Long conceptId,
            @Valid @RequestBody ConceptRequest request) {
        
        ConceptResponse response = conceptService.updateConcept(conceptId, request);
        
        return ResponseEntity.ok(ApiResponse.<ConceptResponse>builder()
                .code("200")
                .message("Concept updated successfully")
                .result(response)
                .build());
    }

    @DeleteMapping("/{conceptId}")
    @Operation(summary = "Delete a concept", description = "Delete a concept by ID")
    public ResponseEntity<ApiResponse<Void>> deleteConcept(@PathVariable Long conceptId) {
        
        conceptService.deleteConcept(conceptId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code("200")
                .message("Concept deleted successfully")
                .build());
    }

    @GetMapping("/node/{nodeId}")
    @Operation(summary = "Get concepts by node", description = "Get all concepts for a specific node")
    public ResponseEntity<ApiResponse<List<ConceptResponse>>> getConceptsByNode(
            @PathVariable Long nodeId) {
        
        List<ConceptResponse> responses = conceptService.getConceptsByNode(nodeId);
        
        return ResponseEntity.ok(ApiResponse.<List<ConceptResponse>>builder()
                .code("200")
                .message("Concepts retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/{conceptId}")
    @Operation(summary = "Get concept by ID", description = "Get a specific concept by its ID")
    public ResponseEntity<ApiResponse<ConceptResponse>> getConceptById(
            @PathVariable Long conceptId) {
        
        ConceptResponse response = conceptService.getConceptById(conceptId);
        
        return ResponseEntity.ok(ApiResponse.<ConceptResponse>builder()
                .code("200")
                .message("Concept retrieved successfully")
                .result(response)
                .build());
    }
}
