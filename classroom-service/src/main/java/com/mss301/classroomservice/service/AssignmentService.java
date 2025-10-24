package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.AssignmentRequest;
import com.mss301.classroomservice.dto.response.AssignmentResponse;

public interface AssignmentService {
    
    AssignmentResponse createAssignment(Long classroomId, AssignmentRequest request, Long teacherId);
    
    AssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request, Long teacherId);
    
    void deleteAssignment(Long assignmentId, Long teacherId);
    
    AssignmentResponse getAssignmentById(Long assignmentId, Long userId);
    
    List<AssignmentResponse> getClassroomAssignments(Long classroomId, Long userId);
    
    List<AssignmentResponse> getTeacherAssignments(Long classroomId, Long teacherId);
    
    void publishAssignment(Long assignmentId, Long teacherId);
    
    void unpublishAssignment(Long assignmentId, Long teacherId);
}
