package com.mss301.classroomservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.classroomservice.dto.request.QuizRequest;
import com.mss301.classroomservice.dto.response.QuizResponse;
import com.mss301.classroomservice.service.QuizService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quiz Management")
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/classroom/{classroomId}")
    @Operation(summary = "Create quiz")
    public ResponseEntity<QuizResponse> createQuiz(
            @PathVariable Long classroomId, @Valid @RequestBody QuizRequest request, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        QuizResponse response = quizService.createQuiz(classroomId, request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update quiz")
    public ResponseEntity<QuizResponse> updateQuiz(
            @PathVariable Long id, @Valid @RequestBody QuizRequest request, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.updateQuiz(id, request, teacherId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete quiz")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        quizService.deleteQuiz(id, teacherId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get quiz by id")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.getQuizById(id, userId));
    }

    @GetMapping("/classroom/{classroomId}")
    @Operation(summary = "Get classroom quizzes")
    public ResponseEntity<List<QuizResponse>> getClassroomQuizzes(
            @PathVariable Long classroomId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.getClassroomQuizzes(classroomId, userId));
    }

    @GetMapping("/classroom/{classroomId}/teacher")
    @Operation(summary = "Get teacher quizzes")
    public ResponseEntity<List<QuizResponse>> getTeacherQuizzes(
            @PathVariable Long classroomId, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.getTeacherQuizzes(classroomId, teacherId));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish quiz")
    public ResponseEntity<Void> publishQuiz(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        quizService.publishQuiz(id, teacherId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish quiz")
    public ResponseEntity<Void> unpublishQuiz(@PathVariable Long id, Authentication authentication) {
        Long teacherId = Long.parseLong(authentication.getName());
        quizService.unpublishQuiz(id, teacherId);
        return ResponseEntity.ok().build();
    }
}
