package com.mss301.contentservice.service;

import java.util.List;

import com.mss301.contentservice.dto.GradeSubmissionRequest;
import com.mss301.contentservice.dto.SubmissionRequest;
import com.mss301.contentservice.dto.SubmissionResponse;

public interface SubmissionService {
    
    List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId);
    
    SubmissionResponse getSubmissionById(Long id, Long userId);
    
    SubmissionResponse submitAssignment(Long assignmentId, Long studentId, String studentName, SubmissionRequest request);
    
    SubmissionResponse gradeSubmission(Long id, Long graderId, GradeSubmissionRequest request);
    
    List<SubmissionResponse> getMySubmissions(Long studentId);
    
    long countSubmissionsByAssignment(Long assignmentId);
    
    long countGradedSubmissions(Long assignmentId);
}
