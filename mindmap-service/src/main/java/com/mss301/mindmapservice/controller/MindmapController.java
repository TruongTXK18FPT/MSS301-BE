package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.request.MindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapResponse;
import com.mss301.mindmapservice.service.MindmapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/mindmap")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mindmap Management", description = "APIs for managing mindmaps")
public class MindmapController {

    private final MindmapService mindmapService;

    @PostMapping
    @Operation(summary = "Create a new mindmap", description = "Create a new mindmap for the authenticated user")
    public ResponseEntity<ApiResponse<MindmapResponse>> createMindmap(
            @Valid @RequestBody MindmapRequest request, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Creating mindmap for user: {}", userId);

        MindmapResponse response = mindmapService.createMindmap(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mindmap created successfully", response));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate mindmap with AI", description = "Generate a mindmap using AI (Mistral or Gemini)")
    public ResponseEntity<ApiResponse<AiGenerateMindmapResponse>> generateMindmapWithAi(
            @Valid @RequestBody AiGenerateMindmapRequest request, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Generating mindmap with AI for user: {}", userId);

        AiGenerateMindmapResponse response = mindmapService.generateMindmapWithAi(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Mindmap generated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mindmap by ID", description = "Get a specific mindmap by its ID")
    public ResponseEntity<ApiResponse<MindmapResponse>> getMindmapById(
            @PathVariable Long id, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting mindmap: {} for user: {}", id, userId);

        MindmapResponse response = mindmapService.getMindmapById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get user's mindmaps", description = "Get all mindmaps for the authenticated user")
    public ResponseEntity<ApiResponse<List<MindmapResponse>>> getUserMindmaps(Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting mindmaps for user: {}", userId);

        List<MindmapResponse> response = mindmapService.getUserMindmaps(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public")
    @Operation(summary = "Get public mindmaps", description = "Get all public mindmaps with pagination")
    public ResponseEntity<ApiResponse<Page<MindmapResponse>>> getPublicMindmaps(Pageable pageable) {

        log.info("Getting public mindmaps");

        Page<MindmapResponse> response = mindmapService.getPublicMindmaps(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search mindmaps", description = "Search mindmaps by keyword")
    public ResponseEntity<ApiResponse<List<MindmapResponse>>> searchMindmaps(
            @RequestParam String keyword, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Searching mindmaps with keyword: {} for user: {}", keyword, userId);

        List<MindmapResponse> response = mindmapService.searchMindmaps(keyword, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update mindmap", description = "Update an existing mindmap")
    public ResponseEntity<ApiResponse<MindmapResponse>> updateMindmap(
            @PathVariable Long id, @Valid @RequestBody MindmapRequest request, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Updating mindmap: {} for user: {}", id, userId);

        MindmapResponse response = mindmapService.updateMindmap(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Mindmap updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete mindmap", description = "Delete a mindmap")
    public ResponseEntity<ApiResponse<Void>> deleteMindmap(@PathVariable Long id, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Deleting mindmap: {} for user: {}", id, userId);

        mindmapService.deleteMindmap(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Mindmap deleted successfully", null));
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Share mindmap", description = "Generate a share code for a mindmap")
    public ResponseEntity<ApiResponse<String>> shareMindmap(@PathVariable Long id, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Sharing mindmap: {} for user: {}", id, userId);

        String shareCode = mindmapService.shareMindmap(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Share code generated", shareCode));
    }

    @GetMapping("/shared/{shareCode}")
    @Operation(summary = "Get shared mindmap", description = "Get a mindmap using share code")
    public ResponseEntity<ApiResponse<MindmapResponse>> getSharedMindmap(@PathVariable String shareCode) {

        log.info("Getting shared mindmap with code: {}", shareCode);

        MindmapResponse response = mindmapService.getSharedMindmap(shareCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user mindmap statistics", description = "Get statistics for user's mindmaps")
    public ResponseEntity<ApiResponse<Object>> getUserMindmapStats(Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting mindmap stats for user: {}", userId);

        Object stats = mindmapService.getUserMindmapStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Extract user ID from JWT token claims
        // This should be implemented based on your JWT structure
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID in token");
        }
    }
}
