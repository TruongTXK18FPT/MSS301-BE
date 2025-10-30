package com.mss301.contentservice.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.mss301.contentservice.dto.ApiResponse;
import com.mss301.contentservice.dto.GradeSubmissionRequest;
import com.mss301.contentservice.dto.SubmissionRequest;
import com.mss301.contentservice.dto.SubmissionResponse;
import com.mss301.contentservice.service.SubmissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/assignment/{assignmentId}")
    public ApiResponse<List<SubmissionResponse>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Jwt jwt) {
        
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByAssignment(assignmentId);
        return ApiResponse.success(submissions);
    }

    @GetMapping("/{id}")
    public ApiResponse<SubmissionResponse> getSubmissionById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = jwt.getClaim("userId");
        SubmissionResponse submission = submissionService.getSubmissionById(id, userId);
        return ApiResponse.success(submission);
    }

    @PostMapping("/assignment/{assignmentId}")
    public ApiResponse<SubmissionResponse> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long studentId = jwt.getClaim("userId");
        String studentName = jwt.getClaim("name");
        
        SubmissionResponse submission = submissionService.submitAssignment(
                assignmentId, studentId, studentName, request);
        return ApiResponse.success("Nộp bài thành công", submission);
    }

    @PutMapping("/{id}/grade")
    public ApiResponse<SubmissionResponse> gradeSubmission(
            @PathVariable Long id,
            @RequestBody GradeSubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long graderId = jwt.getClaim("userId");
        SubmissionResponse submission = submissionService.gradeSubmission(id, graderId, request);
        return ApiResponse.success("Chấm điểm thành công", submission);
    }

    @GetMapping("/my-submissions")
    public ApiResponse<List<SubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal Jwt jwt) {
        
        Long studentId = jwt.getClaim("userId");
        List<SubmissionResponse> submissions = submissionService.getMySubmissions(studentId);
        return ApiResponse.success(submissions);
    }

    @GetMapping("/assignment/{assignmentId}/stats")
    public ApiResponse<SubmissionStats> getSubmissionStats(
            @PathVariable Long assignmentId) {
        
        long totalSubmissions = submissionService.countSubmissionsByAssignment(assignmentId);
        long gradedSubmissions = submissionService.countGradedSubmissions(assignmentId);
        
        SubmissionStats stats = new SubmissionStats(totalSubmissions, gradedSubmissions);
        return ApiResponse.success(stats);
    }

    // Inner class for stats
    public record SubmissionStats(long totalSubmissions, long gradedSubmissions) {}
}
