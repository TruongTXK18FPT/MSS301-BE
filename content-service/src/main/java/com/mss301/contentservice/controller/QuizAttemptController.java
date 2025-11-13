package com.mss301.contentservice.controller;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

    private Long getUserId(Authentication authentication) {
        try {
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                Object uid = jwtAuth.getTokenAttributes().get("userId");
                if (uid == null) {
                    uid = jwtAuth.getTokenAttributes().get("sub");
                }
                if (uid != null) {
                    return Long.parseLong(uid.toString());
                }
            }
            return Long.parseLong(authentication.getName());
        } catch (Exception e) {
            throw new AccessDeniedException("Invalid principal: " + e.getMessage());
        }
    }

    private String getUserName(Authentication authentication) {
        try {
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                Object name = jwtAuth.getTokenAttributes().get("name");
                if (name != null) {
                    return name.toString();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/quiz/{quizId}")
    public ApiResponse<List<QuizAttemptResponse>> getAttemptsByQuiz(
            @PathVariable Long quizId,
            Authentication authentication) {
        
        List<QuizAttemptResponse> attempts = quizAttemptService.getAttemptsByQuiz(quizId);
        return ApiResponse.success(attempts);
    }

    @GetMapping("/quiz/{quizId}/my-attempts")
    public ApiResponse<List<QuizAttemptResponse>> getMyAttempts(
            @PathVariable Long quizId,
            Authentication authentication) {
        
        Long studentId = getUserId(authentication);
        List<QuizAttemptResponse> attempts = quizAttemptService.getMyAttempts(quizId, studentId);
        return ApiResponse.success(attempts);
    }

    @PostMapping("/quiz/{quizId}/start")
    public ApiResponse<QuizAttemptResponse> startQuizAttempt(
            @PathVariable Long quizId,
            Authentication authentication) {
        
        Long studentId = getUserId(authentication);
        if (studentId == null) {
            throw new AccessDeniedException("User ID not found in authentication token");
        }
        
        String studentName = getUserName(authentication);
        if (studentName == null || studentName.trim().isEmpty()) {
            studentName = "Student " + studentId;
        }
        
        QuizAttemptResponse attempt = quizAttemptService.startQuizAttempt(quizId, studentId, studentName);
        return ApiResponse.success("Bắt đầu làm bài", attempt);
    }

    @PutMapping("/{attemptId}/submit")
    public ApiResponse<QuizAttemptResponse> submitQuizAttempt(
            @PathVariable Long attemptId,
            @RequestBody SubmitQuizRequest request,
            Authentication authentication) {
        
        Long studentId = getUserId(authentication);
        if (studentId == null) {
            throw new AccessDeniedException("User ID not found in authentication token");
        }
        
        QuizAttemptResponse attempt = quizAttemptService.submitQuizAttempt(attemptId, studentId, request);
        return ApiResponse.success("Nộp bài thành công", attempt);
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizAttemptResponse> getAttemptById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        if (userId == null) {
            throw new AccessDeniedException("User ID not found in authentication token");
        }
        
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
