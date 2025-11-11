package com.mss301.classroomservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.AssignmentSubmissionRequest;
import com.mss301.classroomservice.dto.request.QuizAttemptRequest;
import com.mss301.classroomservice.entity.AssignmentSubmission;
import com.mss301.classroomservice.entity.ClassroomContent;
import com.mss301.classroomservice.entity.QuizAttempt;
import com.mss301.classroomservice.entity.QuizAttemptAnswer;
import com.mss301.classroomservice.entity.Submission;
import com.mss301.classroomservice.entity.Submission.SubmissionType;
import com.mss301.classroomservice.repository.AssignmentSubmissionRepository;
import com.mss301.classroomservice.repository.ClassroomContentRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.QuizAttemptAnswerRepository;
import com.mss301.classroomservice.repository.QuizAttemptRepository;
import com.mss301.classroomservice.repository.SubmissionRepository;
import com.mss301.classroomservice.service.SubmissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ClassroomContentRepository classroomContentRepository;
    private final ClassroomRepository classroomRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Override
    @Transactional
    public Submission submitQuiz(Long classroomContentId, Long studentId, QuizAttemptRequest request) {
        classroomContentRepository
                .findById(classroomContentId)
                .orElseThrow(() -> new RuntimeException("Classroom content not found"));
        Submission submission = Submission.builder()
                .classroomContentId(classroomContentId)
                .studentId(studentId)
                .type(SubmissionType.QUIZ)
                .submittedAt(LocalDateTime.now())
                .build();
        submission = submissionRepository.save(submission);
        QuizAttempt attempt = QuizAttempt.builder()
                .submissionId(submission.getId())
                .durationSec(request.getDurationSec())
                .rawScore(0)
                .build();
        quizAttemptRepository.save(attempt);
        if (request.getAnswers() != null) {
            for (QuizAttemptRequest.Answer a : request.getAnswers()) {
                QuizAttemptAnswer ans = QuizAttemptAnswer.builder()
                        .attemptId(attempt.getSubmissionId())
                        .questionId(a.getQuestionId())
                        .selectedOptionId(a.getSelectedOptionId())
                        .shortText(a.getShortText())
                        .build();
                quizAttemptAnswerRepository.save(ans);
            }
        }
        return submission;
    }

    @Override
    @Transactional
    public Submission submitAssignment(Long classroomContentId, Long studentId, AssignmentSubmissionRequest request) {
        classroomContentRepository
                .findById(classroomContentId)
                .orElseThrow(() -> new RuntimeException("Classroom content not found"));
        Submission submission = Submission.builder()
                .classroomContentId(classroomContentId)
                .studentId(studentId)
                .type(SubmissionType.ASSIGNMENT)
                .submittedAt(LocalDateTime.now())
                .build();
        submission = submissionRepository.save(submission);
        AssignmentSubmission as = AssignmentSubmission.builder()
                .submissionId(submission.getId())
                .text(request.getText())
                .fileIds(request.getFileIds())
                .build();
        assignmentSubmissionRepository.save(as);
        return submission;
    }

    @Override
    public List<Submission> mySubmissions(Long classroomContentId, Long studentId) {
        return submissionRepository.findByClassroomContentIdAndStudentId(classroomContentId, studentId);
    }

    @Override
    public List<Submission> getAllSubmissions(Long classroomContentId, Long teacherId) {
        // Verify teacher owns the classroom
        ClassroomContent content = classroomContentRepository.findById(classroomContentId)
                .orElseThrow(() -> new RuntimeException("Classroom content not found"));
        
        com.mss301.classroomservice.entity.Classroom classroom = classroomRepository.findById(content.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden: Only classroom owner can view all submissions");
        }
        
        return submissionRepository.findByClassroomContentId(classroomContentId);
    }
}
