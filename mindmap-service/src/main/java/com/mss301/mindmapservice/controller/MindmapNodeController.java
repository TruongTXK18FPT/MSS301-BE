package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.MindmapNodeRequest;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;
import com.mss301.mindmapservice.service.MindmapNodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/mindmap/{mindmapId}/node")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mindmap Node Management", description = "APIs for managing mindmap nodes")
public class MindmapNodeController {

    private final MindmapNodeService mindmapNodeService;

    @PostMapping
    @Operation(summary = "Create a new node", description = "Create a new node in a mindmap")
    public ResponseEntity<ApiResponse<MindmapNodeResponse>> createNode(
            @PathVariable Long mindmapId,
            @Valid @RequestBody MindmapNodeRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Creating node for mindmap: {} by user: {}", mindmapId, userId);

        MindmapNodeResponse response = mindmapNodeService.createNode(request, mindmapId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Node created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get node by ID", description = "Get a specific node by its ID")
    public ResponseEntity<ApiResponse<MindmapNodeResponse>> getNodeById(
            @PathVariable Long mindmapId, @PathVariable Long id, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        MindmapNodeResponse response = mindmapNodeService.getNodeById(id, mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all nodes", description = "Get all nodes for a mindmap")
    public ResponseEntity<ApiResponse<List<MindmapNodeResponse>>> getNodesByMindmapId(
            @PathVariable Long mindmapId, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Getting nodes for mindmap: {} by user: {}", mindmapId, userId);

        List<MindmapNodeResponse> response = mindmapNodeService.getNodesByMindmapId(mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update node", description = "Update an existing node")
    public ResponseEntity<ApiResponse<MindmapNodeResponse>> updateNode(
            @PathVariable Long mindmapId,
            @PathVariable Long id,
            @Valid @RequestBody MindmapNodeRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Updating node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        MindmapNodeResponse response = mindmapNodeService.updateNode(id, request, mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success("Node updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete node", description = "Delete a node")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @PathVariable Long mindmapId, @PathVariable Long id, Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Deleting node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        mindmapNodeService.deleteNode(id, mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success("Node deleted successfully", null));
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "Move node", description = "Move a node to a new position")
    public ResponseEntity<ApiResponse<MindmapNodeResponse>> moveNode(
            @PathVariable Long mindmapId,
            @PathVariable Long id,
            @RequestParam Double x,
            @RequestParam Double y,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Moving node: {} to position ({}, {}) for mindmap: {} by user: {}", id, x, y, mindmapId, userId);

        MindmapNodeResponse response = mindmapNodeService.moveNode(id, x, y, mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success("Node moved successfully", response));
    }

    @PutMapping("/{id}/style")
    @Operation(summary = "Update node style", description = "Update the visual style of a node")
    public ResponseEntity<ApiResponse<MindmapNodeResponse>> updateNodeStyle(
            @PathVariable Long mindmapId,
            @PathVariable Long id,
            @Valid @RequestBody MindmapNodeRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Updating node style: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        MindmapNodeResponse response = mindmapNodeService.updateNodeStyle(id, request, mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success("Node style updated successfully", response));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID in token");
        }
    }
}
