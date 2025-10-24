package com.mss301.ragservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.ragservice.dto.request.RagRequest;
import com.mss301.ragservice.dto.response.RagResponse;
import com.mss301.ragservice.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Service", description = "Retrieval-Augmented Generation API endpoints")
public class RagController {

    private final RagService ragService;

    @Operation(
            summary = "Process RAG Query",
            description =
                    "Submit a query to the RAG service for intelligent document retrieval and AI-powered response generation",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully processed the RAG query",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RagResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request parameters",
                        content = @Content(mediaType = "application/json")),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(mediaType = "application/json"))
            })
    @PostMapping("/query")
    public ResponseEntity<RagResponse> processQuery(
            @Parameter(description = "RAG request containing query, mode, and LLM provider", required = true)
                    @RequestBody
                    RagRequest request) {
        log.info("Received RAG query request - Mode: {}, Provider: {}", request.getMode(), request.getLlmProvider());

        RagResponse response = ragService.processQuery(request);

        log.info("RAG query completed successfully");

        return ResponseEntity.ok(response);
    }
}
