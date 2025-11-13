package com.mss301.contentservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.mss301.contentservice.dto.QuizAttemptResponse;
import com.mss301.contentservice.dto.SubmitQuizRequest;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.QuizAttempt;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.repository.QuizAttemptRepository;
import com.mss301.contentservice.service.QuizAttemptService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final ContentItemRepository contentItemRepository;

    @Override
    public List<QuizAttemptResponse> getAttemptsByQuiz(Long quizId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdOrderByStartedAtDesc(quizId);
        return attempts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizAttemptResponse> getMyAttempts(Long quizId, Long studentId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndStudentIdOrderByStartedAtDesc(quizId, studentId);
        return attempts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuizAttemptResponse startQuizAttempt(Long quizId, Long studentId, String studentName) {
        try {
            // Verify quiz exists
            ContentItem quiz = contentItemRepository.findById(quizId)
                    .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + quizId));

            if (!ContentItem.Type.QUIZ.equals(quiz.getType())) {
                throw new RuntimeException("Content item with id " + quizId + " is not a quiz. Type: " + quiz.getType());
            }

            // Check if there's already an active attempt
            var activeAttempt = quizAttemptRepository.findFirstByQuizIdAndStudentIdAndSubmittedAtIsNullOrderByStartedAtDesc(
                    quizId, studentId);
            
            if (activeAttempt.isPresent()) {
                return toResponse(activeAttempt.get());
            }

            // Create new attempt
            QuizAttempt attempt = QuizAttempt.builder()
                    .quizId(quizId)
                    .studentId(studentId)
                    .studentName(studentName != null ? studentName : "Student " + studentId)
                    .build();

            attempt = quizAttemptRepository.save(attempt);
            return toResponse(attempt);
        } catch (RuntimeException e) {
            throw e; // Re-throw runtime exceptions
        } catch (Exception e) {
            throw new RuntimeException("Failed to start quiz attempt: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public QuizAttemptResponse submitQuizAttempt(Long attemptId, Long studentId, SubmitQuizRequest request) {
        try {
            QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                    .orElseThrow(() -> new RuntimeException("Quiz attempt not found with id: " + attemptId));

            // Verify it's the student's attempt
            if (!attempt.getStudentId().equals(studentId)) {
                throw new RuntimeException("Access denied: Attempt belongs to different student");
            }

            // Check if already submitted
            if (attempt.getSubmittedAt() != null) {
                throw new RuntimeException("Quiz already submitted at: " + attempt.getSubmittedAt());
            }

            // Validate answers
            if (request.getAnswers() == null || request.getAnswers().trim().isEmpty()) {
                throw new RuntimeException("Answers cannot be null or empty");
            }

            // Validate JSON format
            try {
                // Try to parse as JSON to validate format
                new Gson().fromJson(request.getAnswers(), Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid JSON format in answers: " + e.getMessage());
            }

            attempt.setSubmittedAt(LocalDateTime.now());
            attempt.setAnswers(request.getAnswers());
            
            // TODO: Calculate score based on correct answers
            // For now, set score to null (will be graded later or auto-graded)

            attempt = quizAttemptRepository.save(attempt);
            return toResponse(attempt);
        } catch (RuntimeException e) {
            throw e; // Re-throw runtime exceptions
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit quiz attempt: " + e.getMessage(), e);
        }
    }

    @Override
    public QuizAttemptResponse getAttemptById(Long id, Long userId) {
        QuizAttempt attempt = quizAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz attempt not found"));

        // Check if user is the student or the teacher (owner of quiz)
        ContentItem quiz = contentItemRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!attempt.getStudentId().equals(userId) && !quiz.getOwnerId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toResponse(attempt);
    }

    @Override
    public long countAttemptsByQuiz(Long quizId) {
        return quizAttemptRepository.countByQuizId(quizId);
    }

    @Override
    public long countCompletedAttempts(Long quizId) {
        return quizAttemptRepository.countByQuizIdAndSubmittedAtIsNotNull(quizId);
    }

    private QuizAttemptResponse toResponse(QuizAttempt attempt) {
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .quizId(attempt.getQuizId())
                .studentId(attempt.getStudentId())
                .studentName(attempt.getStudentName())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .answers(attempt.getAnswers())
                .build();
    }
}
