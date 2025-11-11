package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.request.ClassroomContentRequest;
import com.mss301.classroomservice.dto.response.ClassroomContentResponse;
import com.mss301.classroomservice.service.ClassroomContentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/classrooms/{classroomId}/contents")
@RequiredArgsConstructor
@Tag(name = "Classroom Content")
public class ClassroomContentController {

    private final ClassroomContentService service;

    @PostMapping
    @Operation(summary = "Attach content to classroom")
    public ResponseEntity<ClassroomContentResponse> attach(
            @PathVariable Long classroomId,
            @Valid @RequestBody ClassroomContentRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.attach(classroomId, request, userId));
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Detach content from classroom")
    public ResponseEntity<Void> detach(
            @PathVariable Long classroomId, @PathVariable("linkId") Long linkId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        service.detach(classroomId, linkId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List classroom contents")
    public ResponseEntity<List<ClassroomContentResponse>> list(
            @PathVariable Long classroomId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.list(classroomId, userId));
    }

    @GetMapping("/student-view")
    @Operation(summary = "List visible classroom contents for students (filtered by publishAt and visible flag)")
    public ResponseEntity<List<ClassroomContentResponse>> listStudentView(
            @PathVariable Long classroomId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.listVisibleContents(classroomId, userId));
    }

    @PostMapping("/create")
    @Operation(summary = "Create new lesson/content directly in classroom (no external content service)")
    public ResponseEntity<ClassroomContentResponse> createContent(
            @PathVariable Long classroomId,
            @Valid @RequestBody ClassroomContentRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createContent(classroomId, request, userId));
    }

    @PutMapping("/update/{contentId}")
    @Operation(summary = "Update existing lesson/content")
    public ResponseEntity<ClassroomContentResponse> updateContent(
            @PathVariable Long classroomId,
            @PathVariable Long contentId,
            @Valid @RequestBody ClassroomContentRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.updateContent(contentId, request, userId));
    }

    @DeleteMapping("/delete/{contentId}")
    @Operation(summary = "Delete lesson/content")
    public ResponseEntity<Void> deleteContent(
            @PathVariable Long classroomId,
            @PathVariable Long contentId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        service.deleteContent(contentId, userId);
        return ResponseEntity.noContent().build();
    }
}
