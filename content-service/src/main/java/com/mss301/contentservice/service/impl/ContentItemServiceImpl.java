package com.mss301.contentservice.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.request.ContentItemRequest;
import com.mss301.contentservice.dto.request.QuizRequestPayload;
import com.mss301.contentservice.dto.request.QuizRequestPayload.QuizOptionRequest;
import com.mss301.contentservice.dto.request.QuizRequestPayload.QuizQuestionRequest;
import com.mss301.contentservice.dto.response.AssignmentDetailResponse;
import com.mss301.contentservice.dto.response.ContentItemResponse;
import com.mss301.contentservice.dto.response.QuizResponsePayload;
import com.mss301.contentservice.dto.response.QuizResponsePayload.QuizOptionDto;
import com.mss301.contentservice.dto.response.QuizResponsePayload.QuizQuestionDto;
import com.mss301.contentservice.entity.AssignmentDetail;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;
import com.mss301.contentservice.entity.Quiz;
import com.mss301.contentservice.entity.QuizOption;
import com.mss301.contentservice.entity.QuizQuestion;
import com.mss301.contentservice.repository.AssignmentDetailRepository;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.repository.QuizOptionRepository;
import com.mss301.contentservice.repository.QuizQuestionRepository;
import com.mss301.contentservice.repository.QuizRepository;
import com.mss301.contentservice.service.ContentItemService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentItemServiceImpl implements ContentItemService {

    private final ContentItemRepository repository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final AssignmentDetailRepository assignmentDetailRepository;

    @Override
    @Transactional
    public ContentItemResponse create(ContentItemRequest request, Long ownerId) {
        ContentItem item = ContentItem.builder()
                .ownerId(ownerId)
                .type(Type.valueOf(request.getType()))
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .subject(request.getSubject())
                .grade(request.getGrade())
                .tags(request.getTags())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();
        item = repository.save(item);
        // Save type-specific details
        if (item.getType() == Type.QUIZ && request.getQuiz() != null) {
            saveQuiz(item.getId(), request.getQuiz());
        } else if (item.getType() == Type.ASSIGNMENT && request.getAssignment() != null) {
            saveAssignment(
                    item.getId(),
                    request.getAssignment().getInstructions(),
                    request.getAssignment().getSubmissionType(),
                    request.getAssignment().getAttachmentFileIds());
        }
        return toResponseWithDetails(item);
    }

    @Override
    @Transactional
    public ContentItemResponse update(Long id, ContentItemRequest request, Long ownerId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        item.setType(Type.valueOf(request.getType()));
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setContent(request.getContent());
        item.setSubject(request.getSubject());
        item.setGrade(request.getGrade());
        item.setTags(request.getTags());
        item.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        item = repository.save(item);
        // Update type-specific details
        if (item.getType() == Type.QUIZ) {
            // clear and re-save for simplicity
            quizRepository.findById(item.getId()).ifPresent(q -> {
                // delete existing questions/options
                List<QuizQuestion> qs = quizQuestionRepository.findByQuizIdOrderByIdAsc(item.getId());
                for (QuizQuestion qq : qs) {
                    quizOptionRepository
                            .findByQuestionIdOrderByIdAsc(qq.getId())
                            .forEach(o -> quizOptionRepository.delete(o));
                    quizQuestionRepository.delete(qq);
                }
                quizRepository.delete(q);
            });
            if (request.getQuiz() != null) {
                saveQuiz(item.getId(), request.getQuiz());
            }
        } else if (item.getType() == Type.ASSIGNMENT) {
            assignmentDetailRepository.findById(item.getId()).ifPresent(assignmentDetailRepository::delete);
            if (request.getAssignment() != null) {
                saveAssignment(
                        item.getId(),
                        request.getAssignment().getInstructions(),
                        request.getAssignment().getSubmissionType(),
                        request.getAssignment().getAttachmentFileIds());
            }
        }
        return toResponseWithDetails(item);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        repository.delete(item);
    }

    @Override
    public ContentItemResponse getById(Long id, Long userId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId) && !Boolean.TRUE.equals(item.getIsPublic())) {
            throw new RuntimeException("Forbidden");
        }
        return toResponseWithDetails(item);
    }

    @Override
    public List<ContentItemResponse> getMyContents(Long ownerId) {
        return repository.findByOwnerId(ownerId).stream()
                .map(this::toResponseWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentItemResponse> getPublicContents() {
        return repository.findByIsPublicTrue().stream()
                .map(this::toResponseWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentItemResponse> searchPublic(String subject, String grade, String keyword) {
        return repository.searchPublic(subject, grade, keyword).stream()
                .map(this::toResponseWithDetails)
                .collect(Collectors.toList());
    }

    private ContentItemResponse toResponseWithDetails(ContentItem item) {
        ContentItemResponse.ContentItemResponseBuilder builder = ContentItemResponse.builder()
                .id(item.getId())
                .ownerId(item.getOwnerId())
                .type(item.getType().name())
                .title(item.getTitle())
                .description(item.getDescription())
                .content(item.getContent())
                .subject(item.getSubject())
                .grade(item.getGrade())
                .tags(item.getTags())
                .isPublic(item.getIsPublic())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt());

        if (item.getType() == Type.QUIZ) {
            quizRepository.findById(item.getId()).ifPresent(q -> {
                List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByIdAsc(item.getId());
                List<QuizQuestionDto> questionDtos = new ArrayList<>();
                for (QuizQuestion qq : questions) {
                    List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByIdAsc(qq.getId());
                    List<QuizOptionDto> optionDtos = options.stream()
                            .map(o -> QuizOptionDto.builder()
                                    .id(o.getId())
                                    .text(o.getText())
                                    .correct(o.getCorrect())
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
                builder.quiz(QuizResponsePayload.builder()
                        .timeLimitSec(q.getTimeLimitSec())
                        .shuffleQuestions(q.getShuffleQuestions())
                        .questions(questionDtos)
                        .build());
            });
        } else if (item.getType() == Type.ASSIGNMENT) {
            assignmentDetailRepository.findById(item.getId()).ifPresent(a -> {
                builder.assignment(AssignmentDetailResponse.builder()
                        .instructions(a.getInstructions())
                        .submissionType(a.getSubmissionType())
                        .attachmentFileIds(a.getAttachmentFileIds())
                        .build());
            });
        }

        return builder.build();
    }

    private void saveQuiz(Long contentItemId, QuizRequestPayload.QuizRequest quizReq) {
        Quiz quiz = Quiz.builder()
                .contentItemId(contentItemId)
                .timeLimitSec(quizReq.getTimeLimitSec())
                .shuffleQuestions(quizReq.getShuffleQuestions())
                .build();
        quizRepository.save(quiz);
        if (quizReq.getQuestions() != null) {
            for (QuizQuestionRequest qreq : quizReq.getQuestions()) {
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
                                .correct(oreq.getCorrect())
                                .build();
                        quizOptionRepository.save(option);
                    }
                }
            }
        }
    }

    private void saveAssignment(
            Long contentItemId, String instructions, String submissionType, String attachmentFileIds) {
        AssignmentDetail detail = AssignmentDetail.builder()
                .contentItemId(contentItemId)
                .instructions(instructions)
                .submissionType(submissionType)
                .attachmentFileIds(attachmentFileIds)
                .build();
        assignmentDetailRepository.save(detail);
    }
}
