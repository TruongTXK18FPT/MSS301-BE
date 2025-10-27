package com.mss301.classroomservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.classroomservice.dto.request.GradeRequest;
import com.mss301.classroomservice.entity.Grade;
import com.mss301.classroomservice.service.GradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/submissions/{submissionId}/grade")
@RequiredArgsConstructor
@Tag(name = "Grading Management")
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    @Operation(summary = "Grade a submission")
    public ResponseEntity<Grade> grade(
            @PathVariable Long submissionId, @Valid @RequestBody GradeRequest request, Authentication authentication) {
        Long graderId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(gradeService.gradeSubmission(submissionId, graderId, request));
    }
}
