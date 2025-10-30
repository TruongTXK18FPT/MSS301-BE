package com.mss301.contentservice.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.request.QuizRequestPayload;
import com.mss301.contentservice.dto.request.QuizRequestPayload.QuizOptionRequest;
import com.mss301.contentservice.dto.request.QuizRequestPayload.QuizQuestionRequest;
import com.mss301.contentservice.dto.response.QuizResponsePayload;
import com.mss301.contentservice.dto.response.QuizResponsePayload.QuizOptionDto;
import com.mss301.contentservice.dto.response.QuizResponsePayload.QuizQuestionDto;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;
import com.mss301.contentservice.entity.Quiz;
import com.mss301.contentservice.entity.QuizOption;
import com.mss301.contentservice.entity.QuizQuestion;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.repository.QuizOptionRepository;
import com.mss301.contentservice.repository.QuizQuestionRepository;
import com.mss301.contentservice.repository.QuizRepository;
import com.mss301.contentservice.service.QuizService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final ContentItemRepository contentItemRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;

    @Override
    public QuizResponsePayload getQuiz(Long contentItemId, Long userId) {
        ContentItem item = contentItemRepository
                .findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        if (item.getType() != Type.QUIZ) {
            throw new RuntimeException("Not a quiz");
        }
        Quiz quiz = quizRepository.findById(contentItemId).orElse(null);
        if (quiz == null) return null;
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByIdAsc(contentItemId);
        List<QuizQuestionDto> questionDtos = new ArrayList<>();
        for (QuizQuestion qq : questions) {
            List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByIdAsc(qq.getId());
            List<QuizOptionDto> optionDtos = options.stream()
                    .map(o -> QuizOptionDto.builder()
                            .id(o.getId())
                            .text(o.getText())
                            .correct(o.getIsCorrect())
                            .build())
                    .collect(Collectors.toList());
            questionDtos.add(QuizQuestionDto.builder()
                    .id(qq.getId())
                    .text(qq.getText())
                    .points(qq.getPoints())
                    .type(qq.getType())
                    .options(optionDtos)
                    .build());
        }
        return QuizResponsePayload.builder()
                .timeLimitSec(quiz.getTimeLimitSec())
                .shuffleQuestions(quiz.getShuffleQuestions())
                .questions(questionDtos)
                .build();
    }

    @Override
    @Transactional
    public QuizResponsePayload putQuiz(Long contentItemId, QuizRequestPayload.QuizRequest request, Long userId) {
        ContentItem item = contentItemRepository
                .findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        if (item.getType() != Type.QUIZ) {
            throw new RuntimeException("Not a quiz");
        }
        // delete and re-create
        quizRepository.findById(contentItemId).ifPresent(q -> {
            List<QuizQuestion> qs = quizQuestionRepository.findByQuizIdOrderByIdAsc(contentItemId);
            for (QuizQuestion qq : qs) {
                quizOptionRepository.findByQuestionIdOrderByIdAsc(qq.getId()).forEach(quizOptionRepository::delete);
                quizQuestionRepository.delete(qq);
            }
            quizRepository.delete(q);
        });
        Quiz quiz = Quiz.builder()
                .contentItemId(contentItemId)
                .timeLimitSec(request.getTimeLimitSec())
                .shuffleQuestions(request.getShuffleQuestions())
                .build();
        quizRepository.save(quiz);
        if (request.getQuestions() != null) {
            for (QuizQuestionRequest qreq : request.getQuestions()) {
                QuizQuestion question = QuizQuestion.builder()
                        .quizId(contentItemId)
                        .text(qreq.getText())
                        .points(qreq.getPoints())
                        .type(qreq.getType())
                        .build();
                question = quizQuestionRepository.save(question);
                if (qreq.getOptions() != null) {
                    for (QuizOptionRequest oreq : qreq.getOptions()) {
                        QuizOption option = QuizOption.builder()
                                .questionId(question.getId())
                                .text(oreq.getText())
                                .isCorrect(oreq.getCorrect())
                                .build();
                        quizOptionRepository.save(option);
                    }
                }
            }
        }
        return getQuiz(contentItemId, userId);
    }
}
