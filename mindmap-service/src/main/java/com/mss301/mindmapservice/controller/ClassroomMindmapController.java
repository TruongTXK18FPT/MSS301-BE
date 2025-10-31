package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.ClassroomMindmapRequest;
import com.mss301.mindmapservice.dto.response.ClassroomMindmapResponse;
import com.mss301.mindmapservice.service.ClassroomMindmapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/classroom")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Classroom Mindmap", description = "APIs for sharing mindmaps to classrooms (Teacher only)")
public class ClassroomMindmapController {

    private final ClassroomMindmapService classroomMindmapService;

    @PostMapping("/share")
    @Operation(summary = "Share mindmap to classroom", description = "Share a mindmap to a classroom (Teacher only)")
    public ResponseEntity<ApiResponse<ClassroomMindmapResponse>> shareMindmapToClassroom(
            @Valid @RequestBody ClassroomMindmapRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long teacherId = Long.parseLong(jwt.getClaim("userId"));
        ClassroomMindmapResponse response = classroomMindmapService.shareMindmapToClassroom(request, teacherId);
        
        return ResponseEntity.ok(ApiResponse.<ClassroomMindmapResponse>builder()
                .code("200")
                .message("Mindmap shared to classroom successfully")
                .result(response)
                .build());
    }

    @DeleteMapping("/{classroomMindmapId}")
    @Operation(summary = "Remove mindmap from classroom", description = "Remove a shared mindmap from classroom")
    public ResponseEntity<ApiResponse<Void>> removeMindmapFromClassroom(
            @PathVariable Long classroomMindmapId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long teacherId = Long.parseLong(jwt.getClaim("userId"));
        classroomMindmapService.removeMindmapFromClassroom(classroomMindmapId, teacherId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code("200")
                .message("Mindmap removed from classroom successfully")
                .build());
    }

    @GetMapping("/classroom/{classroomId}")
    @Operation(summary = "Get mindmaps by classroom", description = "Get all mindmaps shared to a specific classroom")
    public ResponseEntity<ApiResponse<List<ClassroomMindmapResponse>>> getMindmapsByClassroom(
            @PathVariable Long classroomId) {
        
        List<ClassroomMindmapResponse> responses = classroomMindmapService.getMindmapsByClassroom(classroomId);
        
        return ResponseEntity.ok(ApiResponse.<List<ClassroomMindmapResponse>>builder()
                .code("200")
                .message("Classroom mindmaps retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/mindmap/{mindmapId}")
    @Operation(summary = "Get classrooms by mindmap", description = "Get all classrooms a mindmap is shared to")
    public ResponseEntity<ApiResponse<List<ClassroomMindmapResponse>>> getClassroomsByMindmap(
            @PathVariable Long mindmapId) {
        
        List<ClassroomMindmapResponse> responses = classroomMindmapService.getClassroomsByMindmap(mindmapId);
        
        return ResponseEntity.ok(ApiResponse.<List<ClassroomMindmapResponse>>builder()
                .code("200")
                .message("Classrooms retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/teacher/my-shares")
    @Operation(summary = "Get teacher's shared mindmaps", description = "Get all mindmaps shared by the current teacher")
    public ResponseEntity<ApiResponse<List<ClassroomMindmapResponse>>> getMySharedMindmaps(
            @AuthenticationPrincipal Jwt jwt) {
        
        Long teacherId = Long.parseLong(jwt.getClaim("userId"));
        List<ClassroomMindmapResponse> responses = classroomMindmapService.getMindmapsByTeacher(teacherId);
        
        return ResponseEntity.ok(ApiResponse.<List<ClassroomMindmapResponse>>builder()
                .code("200")
                .message("Teacher's shared mindmaps retrieved successfully")
                .result(responses)
                .build());
    }
}
