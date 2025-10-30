package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(
            @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Classroom created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update classroom")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.update(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Classroom updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete classroom")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Classroom deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get classroom by id")
    public ResponseEntity<ApiResponse<ClassroomResponse>> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "My classrooms")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> myClassrooms(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ClassroomResponse> classrooms = classroomService.getMyClassrooms(userId);
        return ResponseEntity.ok(ApiResponse.success(classrooms));
    }

    @GetMapping("/public")
    @Operation(summary = "Public classrooms")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> publicClassrooms() {
        List<ClassroomResponse> classrooms = classroomService.getPublicClassrooms();
        return ResponseEntity.ok(ApiResponse.success(classrooms));
    }

    @PostMapping("/{id}/join-code")
    @Operation(summary = "Generate join code")
    public ResponseEntity<ApiResponse<String>> generateJoinCode(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        String joinCode = classroomService.generateJoinCode(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Join code generated successfully", joinCode));
    }

    @PostMapping("/join/{code}")
    @Operation(summary = "Join classroom by code")
    public ResponseEntity<ApiResponse<ClassroomResponse>> join(@PathVariable("code") String code, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.joinByCode(code, userId);
        return ResponseEntity.ok(ApiResponse.success("Joined classroom successfully", response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search classrooms")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> searchClassrooms(
            @RequestParam String keyword, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ClassroomResponse> classrooms = classroomService.searchClassrooms(keyword);
        return ResponseEntity.ok(ApiResponse.success(classrooms));
    }

    @PostMapping("/join")
    @Operation(summary = "Join classroom with password")
    public ResponseEntity<ApiResponse<ClassroomResponse>> joinClassroom(
            @Valid @RequestBody JoinClassroomRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ClassroomResponse response = classroomService.joinClassroom(request.getClassroomCode(), request.getPassword(), userId);
        return ResponseEntity.ok(ApiResponse.success("Joined classroom successfully", response));
    }

    @GetMapping("/{id}/students")
    @Operation(summary = "Get classroom students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getClassroomStudents(
            @PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        List<StudentResponse> students = classroomService.getClassroomStudents(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @Operation(summary = "Remove student from classroom")
    public ResponseEntity<ApiResponse<Void>> removeStudent(
            @PathVariable Long id, @PathVariable Long studentId, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        classroomService.removeStudentFromClassroom(id, studentId, teacherId);
        return ResponseEntity.ok(ApiResponse.success("Student removed successfully", null));
    }
}
