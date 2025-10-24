package com.mss301.classroomservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.QuizRequest;
import com.mss301.classroomservice.dto.response.QuizResponse;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.entity.Quiz;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.QuizRepository;
import com.mss301.classroomservice.repository.ClassroomMemberRepository;
import com.mss301.classroomservice.service.QuizService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;

    @Override
    @Transactional
    public QuizResponse createQuiz(Long classroomId, QuizRequest request, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .points(request.getPoints())
                .timeLimit(request.getTimeLimit())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .classroomId(classroomId)
                .createdBy(teacherId)
                .isPublished(request.getIsPublished())
                .build();

        quiz = quizRepository.save(quiz);
        return toResponse(quiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizRequest request, Long teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!quiz.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setPoints(request.getPoints());
        quiz.setTimeLimit(request.getTimeLimit());
        quiz.setStartTime(request.getStartTime());
        quiz.setEndTime(request.getEndTime());
        quiz.setIsPublished(request.getIsPublished());

        quiz = quizRepository.save(quiz);
        return toResponse(quiz);
    }

    @Override
    @Transactional
    public void deleteQuiz(Long quizId, Long teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!quiz.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        quizRepository.delete(quiz);
    }

    @Override
    public QuizResponse getQuizById(Long quizId, Long userId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Check if user has access to the classroom
        Classroom classroom = classroomRepository.findById(quiz.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        boolean hasAccess = classroom.getOwnerId().equals(userId) || 
                           classroomMemberRepository.findByClassroomIdAndUserId(quiz.getClassroomId(), userId).isPresent();
        
        if (!hasAccess) {
            throw new RuntimeException("Forbidden");
        }

        return toResponse(quiz);
    }

    @Override
    public List<QuizResponse> getClassroomQuizzes(Long classroomId, Long userId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        // Check if user has access to the classroom
        boolean hasAccess = classroom.getOwnerId().equals(userId) || 
                           classroomMemberRepository.findByClassroomIdAndUserId(classroomId, userId).isPresent();
        
        if (!hasAccess) {
            throw new RuntimeException("Forbidden");
        }

        return quizRepository.findByClassroomId(classroomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizResponse> getTeacherQuizzes(Long classroomId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        return quizRepository.findByClassroomIdAndTeacher(classroomId, teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void publishQuiz(Long quizId, Long teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!quiz.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        quiz.setIsPublished(true);
        quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public void unpublishQuiz(Long quizId, Long teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!quiz.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        quiz.setIsPublished(false);
        quizRepository.save(quiz);
    }

    private QuizResponse toResponse(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .points(quiz.getPoints())
                .timeLimit(quiz.getTimeLimit())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .isPublished(quiz.getIsPublished())
                .classroomId(quiz.getClassroomId())
                .createdBy(quiz.getCreatedBy())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}
