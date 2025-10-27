package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.request.JoinClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
import com.mss301.classroomservice.dto.response.StudentResponse;
import com.mss301.classroomservice.service.ClassroomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/classrooms")
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

    @GetMapping("/search")
    @Operation(summary = "Search classrooms")
    public ResponseEntity<List<ClassroomResponse>> searchClassrooms(
            @RequestParam String keyword, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.searchClassrooms(keyword));
    }

    @PostMapping("/join")
    @Operation(summary = "Join classroom with password")
    public ResponseEntity<ClassroomResponse> joinClassroom(
            @Valid @RequestBody JoinClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.joinClassroom(request.getClassroomCode(), request.getPassword(), userId));
    }

    @GetMapping("/{id}/students")
    @Operation(summary = "Get classroom students")
    public ResponseEntity<List<StudentResponse>> getClassroomStudents(
            @PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.getClassroomStudents(id, teacherId));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @Operation(summary = "Remove student from classroom")
    public ResponseEntity<Void> removeStudent(
            @PathVariable Long id, @PathVariable Long studentId, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        classroomService.removeStudentFromClassroom(id, studentId, teacherId);
        return ResponseEntity.noContent().build();
    }
}
