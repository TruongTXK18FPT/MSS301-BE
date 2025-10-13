package com.mss301.classroomservice.service;

import com.mss301.classroomservice.dto.request.GradeRequest;
import com.mss301.classroomservice.entity.Grade;

public interface GradeService {

    Grade gradeSubmission(Long submissionId, Long graderId, GradeRequest request);
}
