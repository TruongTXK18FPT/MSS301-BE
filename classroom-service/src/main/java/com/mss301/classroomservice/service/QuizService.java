package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.QuizRequest;
import com.mss301.classroomservice.dto.response.QuizResponse;

public interface QuizService {
    
    QuizResponse createQuiz(Long classroomId, QuizRequest request, Long teacherId);
    
    QuizResponse updateQuiz(Long quizId, QuizRequest request, Long teacherId);
    
    void deleteQuiz(Long quizId, Long teacherId);
    
    QuizResponse getQuizById(Long quizId, Long userId);
    
    List<QuizResponse> getClassroomQuizzes(Long classroomId, Long userId);
    
    List<QuizResponse> getTeacherQuizzes(Long classroomId, Long teacherId);
    
    void publishQuiz(Long quizId, Long teacherId);
    
    void unpublishQuiz(Long quizId, Long teacherId);
}
