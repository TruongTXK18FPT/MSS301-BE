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
}
