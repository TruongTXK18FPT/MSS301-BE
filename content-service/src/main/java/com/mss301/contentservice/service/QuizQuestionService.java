package com.mss301.contentservice.service;

import java.util.List;

import com.mss301.contentservice.dto.request.QuizQuestionRequest;
import com.mss301.contentservice.dto.response.QuizQuestionResponse;

public interface QuizQuestionService {
    List<QuizQuestionResponse> getQuestionsByQuizId(Long quizId, Long userId);
    QuizQuestionResponse getQuestionById(Long questionId, Long userId);
    QuizQuestionResponse addQuestion(Long quizId, QuizQuestionRequest request, Long userId);
    QuizQuestionResponse updateQuestion(Long questionId, QuizQuestionRequest request, Long userId);
    void deleteQuestion(Long questionId, Long userId);
    List<QuizQuestionResponse> addQuestions(Long quizId, List<QuizQuestionRequest> requests, Long userId);
}
