package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.classroomservice.dto.request.AssignmentSubmissionRequest;
import com.mss301.classroomservice.dto.request.QuizAttemptRequest;
import com.mss301.classroomservice.entity.Submission;
import com.mss301.classroomservice.service.SubmissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/classrooms/contents/{id}/submissions")
@RequiredArgsConstructor
@Tag(name = "Submission Management")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/quiz")
    @Operation(summary = "Submit a quiz attempt")
    public ResponseEntity<Submission> submitQuiz(
            @PathVariable("id") Long classroomContentId,
            @Valid @RequestBody QuizAttemptRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(submissionService.submitQuiz(classroomContentId, userId, request));
    }

    @PostMapping("/assignment")
    @Operation(summary = "Submit an assignment")
    public ResponseEntity<Submission> submitAssignment(
            @PathVariable("id") Long classroomContentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(submissionService.submitAssignment(classroomContentId, userId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "List my submissions for this classroom content")
    public ResponseEntity<List<Submission>> mySubmissions(
            @PathVariable("id") Long classroomContentId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(submissionService.mySubmissions(classroomContentId, userId));
    }
}
