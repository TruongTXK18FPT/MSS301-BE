package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.AssignmentSubmissionRequest;
import com.mss301.classroomservice.dto.request.QuizAttemptRequest;
import com.mss301.classroomservice.entity.Submission;

public interface SubmissionService {

    Submission submitQuiz(Long classroomContentId, Long studentId, QuizAttemptRequest request);

    Submission submitAssignment(Long classroomContentId, Long studentId, AssignmentSubmissionRequest request);

    List<Submission> mySubmissions(Long classroomContentId, Long studentId);
}
