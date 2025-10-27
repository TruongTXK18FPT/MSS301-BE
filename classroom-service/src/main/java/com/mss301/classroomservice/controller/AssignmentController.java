package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.request.AssignmentRequest;
import com.mss301.classroomservice.dto.response.AssignmentResponse;
import com.mss301.classroomservice.service.AssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignment Management")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/classroom/{classroomId}")
    @Operation(summary = "Create assignment")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long classroomId, @Valid @RequestBody AssignmentRequest request, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        AssignmentResponse response = assignmentService.createAssignment(classroomId, request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update assignment")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id, @Valid @RequestBody AssignmentRequest request, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request, teacherId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete assignment")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        assignmentService.deleteAssignment(id, teacherId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assignment by id")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.getAssignmentById(id, userId));
    }

    @GetMapping("/classroom/{classroomId}")
    @Operation(summary = "Get classroom assignments")
    public ResponseEntity<List<AssignmentResponse>> getClassroomAssignments(
            @PathVariable Long classroomId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.getClassroomAssignments(classroomId, userId));
    }

    @GetMapping("/classroom/{classroomId}/teacher")
    @Operation(summary = "Get teacher assignments")
    public ResponseEntity<List<AssignmentResponse>> getTeacherAssignments(
            @PathVariable Long classroomId, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(assignmentService.getTeacherAssignments(classroomId, teacherId));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish assignment")
    public ResponseEntity<Void> publishAssignment(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        assignmentService.publishAssignment(id, teacherId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish assignment")
    public ResponseEntity<Void> unpublishAssignment(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        assignmentService.unpublishAssignment(id, teacherId);
        return ResponseEntity.ok().build();
    }
}
