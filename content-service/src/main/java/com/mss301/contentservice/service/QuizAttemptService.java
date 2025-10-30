package com.mss301.contentservice.service;

import java.util.List;

import com.mss301.contentservice.dto.QuizAttemptResponse;
import com.mss301.contentservice.dto.SubmitQuizRequest;

public interface QuizAttemptService {
    
    List<QuizAttemptResponse> getAttemptsByQuiz(Long quizId);
    
    List<QuizAttemptResponse> getMyAttempts(Long quizId, Long studentId);
    
    QuizAttemptResponse startQuizAttempt(Long quizId, Long studentId, String studentName);
    
    QuizAttemptResponse submitQuizAttempt(Long attemptId, Long studentId, SubmitQuizRequest request);
    
    QuizAttemptResponse getAttemptById(Long id, Long userId);
    
    long countAttemptsByQuiz(Long quizId);
    
    long countCompletedAttempts(Long quizId);
}
