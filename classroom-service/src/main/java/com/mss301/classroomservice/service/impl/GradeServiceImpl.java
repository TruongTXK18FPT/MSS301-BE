package com.mss301.classroomservice.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.mss301.classroomservice.dto.request.GradeRequest;
import com.mss301.classroomservice.entity.Grade;
import com.mss301.classroomservice.entity.Submission;
import com.mss301.classroomservice.repository.GradeRepository;
import com.mss301.classroomservice.repository.SubmissionRepository;
import com.mss301.classroomservice.service.GradeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;

    @Override
    public Grade gradeSubmission(Long submissionId, Long graderId, GradeRequest request) {
        Submission sub = submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        Grade grade = Grade.builder()
                .submissionId(sub.getId())
                .points(request.getPoints())
                .feedback(request.getFeedback())
                .graderId(graderId)
                .gradedAt(LocalDateTime.now())
                .build();
        return gradeRepository.save(grade);
    }
}
