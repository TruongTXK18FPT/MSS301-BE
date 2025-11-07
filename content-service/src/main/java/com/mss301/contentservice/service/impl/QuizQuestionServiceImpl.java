package com.mss301.contentservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.request.QuizQuestionRequest;
import com.mss301.contentservice.dto.response.QuizQuestionResponse;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.QuizOption;
import com.mss301.contentservice.entity.QuizQuestion;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.repository.QuizOptionRepository;
import com.mss301.contentservice.repository.QuizQuestionRepository;
import com.mss301.contentservice.service.QuizQuestionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizQuestionServiceImpl implements QuizQuestionService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final ContentItemRepository contentItemRepository;

    @Override
    public List<QuizQuestionResponse> getQuestionsByQuizId(Long quizId, Long userId) {
        // Verify user has access to this quiz
        ContentItem quiz = contentItemRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByIdAsc(quizId);
        return questions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public QuizQuestionResponse getQuestionById(Long questionId, Long userId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return toResponse(question);
    }

    @Override
    @Transactional
    public QuizQuestionResponse addQuestion(Long quizId, QuizQuestionRequest request, Long userId) {
        // Verify quiz exists and user is owner
        ContentItem quiz = contentItemRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!quiz.getOwnerId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Create question
        QuizQuestion question = QuizQuestion.builder()
                .quizId(quizId)
                .text(request.getQuestionText())
                .type(request.getQuestionType().name())
                .points(request.getPoints())
                .explanation(request.getExplanation())
                .orderIndex(request.getOrderIndex())
                .build();
        
        question = quizQuestionRepository.save(question);

        // Create options if provided
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            Long questionId = question.getId();
            for (int i = 0; i < request.getOptions().size(); i++) {
                QuizQuestionRequest.QuizOptionRequest optReq = request.getOptions().get(i);
                QuizOption option = QuizOption.builder()
                        .questionId(questionId)
                        .text(optReq.getOptionText())
                        .isCorrect(optReq.getIsCorrect())
                        .orderIndex(optReq.getOrderIndex() != null ? optReq.getOrderIndex() : i)
                        .build();
                quizOptionRepository.save(option);
            }
        }

        return toResponse(question);
    }

    @Override
    @Transactional
    public QuizQuestionResponse updateQuestion(Long questionId, QuizQuestionRequest request, Long userId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Verify ownership
        ContentItem quiz = contentItemRepository.findById(question.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getOwnerId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Update question
        question.setText(request.getQuestionText());
        question.setType(request.getQuestionType().name());
        question.setPoints(request.getPoints());
        question.setExplanation(request.getExplanation());
        if (request.getOrderIndex() != null) {
            question.setOrderIndex(request.getOrderIndex());
        }
        question = quizQuestionRepository.save(question);

        // Update options - delete old ones and create new
        quizOptionRepository.findByQuestionIdOrderByIdAsc(questionId)
                .forEach(quizOptionRepository::delete);
        
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            for (int i = 0; i < request.getOptions().size(); i++) {
                QuizQuestionRequest.QuizOptionRequest optReq = request.getOptions().get(i);
                QuizOption option = QuizOption.builder()
                        .questionId(questionId)
                        .text(optReq.getOptionText())
                        .isCorrect(optReq.getIsCorrect())
                        .orderIndex(optReq.getOrderIndex() != null ? optReq.getOrderIndex() : i)
                        .build();
                quizOptionRepository.save(option);
            }
        }

        return toResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Verify ownership
        ContentItem quiz = contentItemRepository.findById(question.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getOwnerId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Delete options first
        quizOptionRepository.findByQuestionIdOrderByIdAsc(questionId)
                .forEach(quizOptionRepository::delete);
        
        // Delete question
        quizQuestionRepository.delete(question);
    }

    @Override
    @Transactional
    public List<QuizQuestionResponse> addQuestions(Long quizId, List<QuizQuestionRequest> requests, Long userId) {
        return requests.stream()
                .map(request -> addQuestion(quizId, request, userId))
                .collect(Collectors.toList());
    }

    private QuizQuestionResponse toResponse(QuizQuestion question) {
        List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByIdAsc(question.getId());
        
        return QuizQuestionResponse.builder()
                .id(question.getId())
                .quizId(question.getQuizId())
                .questionText(question.getText())
                .questionType(question.getType())
                .points(question.getPoints())
                .explanation(question.getExplanation())
                .orderIndex(question.getOrderIndex())
                .options(options.stream()
                        .map(opt -> QuizQuestionResponse.QuizOptionResponse.builder()
                                .id(opt.getId())
                                .optionText(opt.getText())
                                .isCorrect(opt.getIsCorrect())
                                .orderIndex(opt.getOrderIndex())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
