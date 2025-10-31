package com.mss301.contentservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.GradeSubmissionRequest;
import com.mss301.contentservice.dto.SubmissionRequest;
import com.mss301.contentservice.dto.SubmissionResponse;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.Submission;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.repository.SubmissionRepository;
import com.mss301.contentservice.service.SubmissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ContentItemRepository contentItemRepository;

    @Override
    public List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId) {
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        return submissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubmissionResponse getSubmissionById(Long id, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Check if user is the student or the teacher (owner of assignment)
        ContentItem assignment = contentItemRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!submission.getStudentId().equals(userId) && !assignment.getOwnerId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toResponse(submission);
    }

    @Override
    @Transactional
    public SubmissionResponse submitAssignment(Long assignmentId, Long studentId, String studentName, SubmissionRequest request) {
        // Verify assignment exists
        ContentItem assignment = contentItemRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!ContentItem.Type.ASSIGNMENT.equals(assignment.getType())) {
            throw new RuntimeException("Content item is not an assignment");
        }

        // Check if already submitted
        List<Submission> existingSubmissions = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        if (!existingSubmissions.isEmpty()) {
            throw new RuntimeException("Assignment already submitted");
        }

        // Determine status (late or pending)
        Submission.Status status = Submission.Status.PENDING;
        if (assignment.getDueDate() != null && LocalDateTime.now().isAfter(assignment.getDueDate())) {
            status = Submission.Status.LATE;
        }

        Submission submission = Submission.builder()
                .assignmentId(assignmentId)
                .studentId(studentId)
                .studentName(studentName)
                .content(request.getContent())
                .fileIds(request.getFileIds())
                .status(status)
                .build();

        submission = submissionRepository.save(submission);
        return toResponse(submission);
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(Long id, Long graderId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Verify grader is the owner of the assignment
        ContentItem assignment = contentItemRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!assignment.getOwnerId().equals(graderId)) {
            throw new RuntimeException("Only assignment owner can grade submissions");
        }

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(graderId);
        submission.setStatus(Submission.Status.GRADED);

        submission = submissionRepository.save(submission);
        return toResponse(submission);
    }

    @Override
    public List<SubmissionResponse> getMySubmissions(Long studentId) {
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        return submissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countSubmissionsByAssignment(Long assignmentId) {
        return submissionRepository.countByAssignmentId(assignmentId);
    }

    @Override
    public long countGradedSubmissions(Long assignmentId) {
        return submissionRepository.countByAssignmentIdAndStatus(assignmentId, Submission.Status.GRADED);
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignmentId())
                .studentId(submission.getStudentId())
                .studentName(submission.getStudentName())
                .content(submission.getContent())
                .fileIds(submission.getFileIds())
                .submittedAt(submission.getSubmittedAt())
                .grade(submission.getGrade())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .gradedBy(submission.getGradedBy())
                .status(submission.getStatus())
                .build();
    }
}
