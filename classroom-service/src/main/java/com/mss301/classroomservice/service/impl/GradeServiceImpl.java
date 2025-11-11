package com.mss301.classroomservice.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.mss301.classroomservice.dto.request.GradeRequest;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.entity.ClassroomContent;
import com.mss301.classroomservice.entity.Grade;
import com.mss301.classroomservice.entity.Submission;
import com.mss301.classroomservice.repository.ClassroomContentRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.GradeRepository;
import com.mss301.classroomservice.repository.SubmissionRepository;
import com.mss301.classroomservice.service.GradeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    private final ClassroomContentRepository classroomContentRepository;
    private final ClassroomRepository classroomRepository;

    @Override
    public Grade gradeSubmission(Long submissionId, Long graderId, GradeRequest request) {
        // 1. Find submission
        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));

        // 2. Find classroom content to get classroom
        ClassroomContent content = classroomContentRepository
                .findById(submission.getClassroomContentId())
                .orElseThrow(() -> new RuntimeException("Classroom content not found with id: " + submission.getClassroomContentId()));

        // 3. Find classroom to verify ownership
        Classroom classroom = classroomRepository
                .findById(content.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found with id: " + content.getClassroomId()));

        // 4. Permission check: only classroom owner can grade
        if (!classroom.getOwnerId().equals(graderId)) {
            throw new RuntimeException("Forbidden: Only classroom owner can grade submissions. " +
                    "Classroom owner: " + classroom.getOwnerId() + ", Grader: " + graderId);
        }

        // 5. Validate score
        if (request.getPoints() < 0) {
            throw new RuntimeException("Invalid score: points cannot be negative");
        }

        // 6. Check if grade already exists for this submission
        Grade existingGrade = gradeRepository.findBySubmissionId(submissionId).orElse(null);

        Grade grade;
        if (existingGrade != null) {
            // Update existing grade
            existingGrade.setPoints(request.getPoints());
            existingGrade.setFeedback(request.getFeedback());
            existingGrade.setGraderId(graderId);
            existingGrade.setGradedAt(LocalDateTime.now());
            grade = gradeRepository.save(existingGrade);
        } else {
            // Create new grade
            grade = Grade.builder()
                    .submissionId(submission.getId())
                    .studentId(submission.getStudentId())
                    .points(request.getPoints())
                    .feedback(request.getFeedback())
                    .graderId(graderId)
                    .gradedAt(LocalDateTime.now())
                    .build();
            grade = gradeRepository.save(grade);
        }

        // 7. Update submission status to GRADED
        submission.setStatus("GRADED");
        submissionRepository.save(submission);

        return grade;
    }
}
