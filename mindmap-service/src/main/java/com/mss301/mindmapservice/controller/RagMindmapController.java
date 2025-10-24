package com.mss301.mindmapservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.RagMindmapRequest;
import com.mss301.mindmapservice.dto.response.RagMindmapResponse;
import com.mss301.mindmapservice.service.RagMindmapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/rag-mindmap")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG Mindmap Services", description = "APIs for RAG-enhanced mindmap generation")
public class RagMindmapController {

    private final RagMindmapService ragMindmapService;

    @PostMapping("/generate")
    @Operation(
            summary = "Generate mindmap using RAG",
            description = "Generate a mindmap using RAG (Retrieval-Augmented Generation) with document context")
    public ResponseEntity<ApiResponse<RagMindmapResponse>> generateRagMindmap(
            @Parameter(description = "RAG mindmap generation request", required = true) @Valid @RequestBody
                    RagMindmapRequest request,
            @Parameter(description = "User ID", required = true) @RequestHeader("X-User-Id") Long userId) {

        log.info("Generating RAG mindmap for user: {} with topic: {}", userId, request.getTopic());

        try {
            RagMindmapResponse response = ragMindmapService.generateRagMindmap(request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Failed to generate RAG mindmap: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Failed to generate RAG mindmap: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Check RAG service health", description = "Check if RAG service is available and healthy")
    public ResponseEntity<ApiResponse<Boolean>> checkRagServiceHealth() {

        log.info("Checking RAG service health");

        boolean isHealthy = ragMindmapService.isRagServiceAvailable();
        return ResponseEntity.ok(ApiResponse.success(isHealthy));
    }

    @PostMapping("/documents")
    @Operation(summary = "Get relevant documents", description = "Get relevant documents for a given topic using RAG")
    public ResponseEntity<ApiResponse<String>> getRelevantDocuments(
            @Parameter(description = "RAG mindmap request", required = true) @Valid @RequestBody
                    RagMindmapRequest request) {

        log.info("Getting relevant documents for topic: {}", request.getTopic());

        try {
            String documents = ragMindmapService.getRelevantDocuments(request);

            if (documents != null && !documents.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(documents));
            } else {
                return ResponseEntity.ok(ApiResponse.success("No relevant documents found"));
            }

        } catch (Exception e) {
            log.error("Failed to get relevant documents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Failed to get relevant documents: " + e.getMessage()));
        }
    }
}
