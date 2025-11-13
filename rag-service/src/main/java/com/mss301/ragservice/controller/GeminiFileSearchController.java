package com.mss301.ragservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.ragservice.dto.request.FileSearchQueryRequest;
import com.mss301.ragservice.dto.response.FileSearchQueryResponse;
import com.mss301.ragservice.dto.response.FileSearchStoreResponse;
import com.mss301.ragservice.service.GeminiFileSearchService;

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
@RequestMapping("/api/v1/file-search")
@RequiredArgsConstructor
@Tag(name = "Gemini File Search", description = "Gemini File Search API endpoints for RAG with uploaded files")
public class GeminiFileSearchController {

    private final GeminiFileSearchService fileSearchService;

    @Operation(
            summary = "List File Search Stores",
            description = "Retrieve a list of all available file search stores from Gemini API",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved file search stores",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FileSearchStoreResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(mediaType = "application/json"))
            })
    @GetMapping("/stores")
    public ResponseEntity<List<FileSearchStoreResponse>> listFileSearchStores() {
        log.info("Received request to list file search stores");

        List<FileSearchStoreResponse> stores = fileSearchService.listFileSearchStores();

        log.info("Successfully retrieved {} file search stores", stores.size());
        return ResponseEntity.ok(stores);
    }

    @Operation(
            summary = "Query with File Search",
            description =
                    "Submit a query to a specific file search store to get AI-powered answers based on uploaded documents",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully processed the file search query",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FileSearchQueryResponse.class))),
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
    public ResponseEntity<FileSearchQueryResponse> queryWithFileSearch(
            @Parameter(description = "File search query request containing store name and query text", required = true)
                    @RequestBody
                    FileSearchQueryRequest request) {
        log.info(
                "Received file search query - Store: {}, Query: '{}'",
                request.getFileStoreName(),
                request.getQuery());

        FileSearchQueryResponse response =
                fileSearchService.queryWithFileSearch(request.getFileStoreName(), request.getQuery());

        log.info("File search query completed successfully");
        return ResponseEntity.ok(response);
    }
}

