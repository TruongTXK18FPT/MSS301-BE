package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
import com.mss301.classroomservice.service.ClassroomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classroom Management")
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    @Operation(summary = "Create classroom")
    public ResponseEntity<ClassroomResponse> create(
            @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update classroom")
    public ResponseEntity<ClassroomResponse> update(
            @PathVariable Long id, @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete classroom")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get classroom by id")
    public ResponseEntity<ClassroomResponse> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.getById(id, userId));
    }

    @GetMapping("/me")
    @Operation(summary = "My classrooms")
    public ResponseEntity<List<ClassroomResponse>> myClassrooms(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.getMyClassrooms(userId));
    }

    @GetMapping("/public")
    @Operation(summary = "Public classrooms")
    public ResponseEntity<List<ClassroomResponse>> publicClassrooms() {
        return ResponseEntity.ok(classroomService.getPublicClassrooms());
    }

    @PostMapping("/{id}/join-code")
    @Operation(summary = "Generate join code")
    public ResponseEntity<String> generateJoinCode(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.generateJoinCode(id, userId));
    }

    @PostMapping("/join/{code}")
    @Operation(summary = "Join classroom by code")
    public ResponseEntity<ClassroomResponse> join(@PathVariable("code") String code, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.joinByCode(code, userId));
    }
}
