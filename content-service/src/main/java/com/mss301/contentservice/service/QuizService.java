package com.mss301.contentservice.service;

import com.mss301.contentservice.dto.request.QuizRequestPayload;
import com.mss301.contentservice.dto.response.QuizResponsePayload;

public interface QuizService {

    QuizResponsePayload getQuiz(Long contentItemId, Long userId);

    QuizResponsePayload putQuiz(Long contentItemId, QuizRequestPayload.QuizRequest request, Long userId);
}
