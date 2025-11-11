package com.mss301.contentservice.service;

import java.util.List;

import com.mss301.contentservice.dto.request.GenerateQuizRequest;
import com.mss301.contentservice.dto.request.QuizRequestPayload;

public interface AiQuizService {
    List<QuizRequestPayload.QuizQuestionRequest> generateQuizQuestions(
            GenerateQuizRequest request, List<String> existingQuestions);
}
