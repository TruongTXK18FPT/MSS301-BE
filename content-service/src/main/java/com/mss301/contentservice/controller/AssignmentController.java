package com.mss301.contentservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.contentservice.dto.request.AssignmentDetailRequest;
import com.mss301.contentservice.dto.response.AssignmentDetailResponse;
import com.mss301.contentservice.service.AssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contents/{id}/assignment")
@RequiredArgsConstructor
@Tag(name = "Assignment Management")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    @Operation(summary = "Get assignment definition for a content item")
    public ResponseEntity<AssignmentDetailResponse> get(
            @PathVariable("id") Long contentItemId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.getAssignment(contentItemId, userId));
    }

    @PutMapping
    @Operation(summary = "Create or replace assignment definition for a content item")
    public ResponseEntity<AssignmentDetailResponse> put(
            @PathVariable("id") Long contentItemId,
            @Valid @RequestBody AssignmentDetailRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.putAssignment(contentItemId, request, userId));
    }
}
