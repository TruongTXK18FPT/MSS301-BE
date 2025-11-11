package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.AssignmentSubmissionRequest;
import com.mss301.classroomservice.dto.request.QuizAttemptRequest;
import com.mss301.classroomservice.entity.Submission;

public interface SubmissionService {

    Submission submitQuiz(Long classroomContentId, Long studentId, QuizAttemptRequest request);

    Submission submitAssignment(Long classroomContentId, Long studentId, AssignmentSubmissionRequest request);

    List<Submission> mySubmissions(Long classroomContentId, Long studentId);
    
    // Teacher: Get all submissions for a classroom content (assignment or quiz)
    List<Submission> getAllSubmissions(Long classroomContentId, Long teacherId);
}
