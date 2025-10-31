package com.mss301.contentservice.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.mss301.contentservice.dto.ApiResponse;
import com.mss301.contentservice.dto.QuizAttemptResponse;
import com.mss301.contentservice.dto.SubmitQuizRequest;
import com.mss301.contentservice.service.QuizAttemptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @GetMapping("/quiz/{quizId}")
    public ApiResponse<List<QuizAttemptResponse>> getAttemptsByQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal Jwt jwt) {
        
        List<QuizAttemptResponse> attempts = quizAttemptService.getAttemptsByQuiz(quizId);
        return ApiResponse.success(attempts);
    }

    @GetMapping("/quiz/{quizId}/my-attempts")
    public ApiResponse<List<QuizAttemptResponse>> getMyAttempts(
            @PathVariable Long quizId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long studentId = jwt.getClaim("userId");
        List<QuizAttemptResponse> attempts = quizAttemptService.getMyAttempts(quizId, studentId);
        return ApiResponse.success(attempts);
    }

    @PostMapping("/quiz/{quizId}/start")
    public ApiResponse<QuizAttemptResponse> startQuizAttempt(
            @PathVariable Long quizId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long studentId = jwt.getClaim("userId");
        String studentName = jwt.getClaim("name");
        
        QuizAttemptResponse attempt = quizAttemptService.startQuizAttempt(quizId, studentId, studentName);
        return ApiResponse.success("Bắt đầu làm bài", attempt);
    }

    @PutMapping("/{attemptId}/submit")
    public ApiResponse<QuizAttemptResponse> submitQuizAttempt(
            @PathVariable Long attemptId,
            @RequestBody SubmitQuizRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long studentId = jwt.getClaim("userId");
        QuizAttemptResponse attempt = quizAttemptService.submitQuizAttempt(attemptId, studentId, request);
        return ApiResponse.success("Nộp bài thành công", attempt);
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizAttemptResponse> getAttemptById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = jwt.getClaim("userId");
        QuizAttemptResponse attempt = quizAttemptService.getAttemptById(id, userId);
        return ApiResponse.success(attempt);
    }

    @GetMapping("/quiz/{quizId}/stats")
    public ApiResponse<QuizStats> getQuizStats(@PathVariable Long quizId) {
        long totalAttempts = quizAttemptService.countAttemptsByQuiz(quizId);
        long completedAttempts = quizAttemptService.countCompletedAttempts(quizId);
        
        QuizStats stats = new QuizStats(totalAttempts, completedAttempts);
        return ApiResponse.success(stats);
    }

    // Inner class for stats
    public record QuizStats(long totalAttempts, long completedAttempts) {}
}
