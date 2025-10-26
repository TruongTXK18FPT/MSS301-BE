package com.mss301.classroomservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.AssignmentRequest;
import com.mss301.classroomservice.dto.response.AssignmentResponse;
import com.mss301.classroomservice.entity.Assignment;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.repository.AssignmentRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.ClassroomMemberRepository;
import com.mss301.classroomservice.service.AssignmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(Long classroomId, AssignmentRequest request, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .points(request.getPoints())
                .dueDate(request.getDueDate())
                .classroomId(classroomId)
                .createdBy(teacherId)
                .isPublished(request.getIsPublished())
                .build();

        assignment = assignmentRepository.save(assignment);
        return toResponse(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setPoints(request.getPoints());
        assignment.setDueDate(request.getDueDate());
        assignment.setIsPublished(request.getIsPublished());

        assignment = assignmentRepository.save(assignment);
        return toResponse(assignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        assignmentRepository.delete(assignment);
    }

    @Override
    public AssignmentResponse getAssignmentById(Long assignmentId, Long userId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        // Check if user has access to the classroom
        Classroom classroom = classroomRepository.findById(assignment.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        boolean hasAccess = classroom.getOwnerId().equals(userId) || 
                           classroomMemberRepository.findByClassroomIdAndUserId(assignment.getClassroomId(), userId).isPresent();
        
        if (!hasAccess) {
            throw new RuntimeException("Forbidden");
        }

        return toResponse(assignment);
    }

    @Override
    public List<AssignmentResponse> getClassroomAssignments(Long classroomId, Long userId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        // Check if user has access to the classroom
        boolean hasAccess = classroom.getOwnerId().equals(userId) || 
                           classroomMemberRepository.findByClassroomIdAndUserId(classroomId, userId).isPresent();
        
        if (!hasAccess) {
            throw new RuntimeException("Forbidden");
        }

        return assignmentRepository.findByClassroomId(classroomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssignmentResponse> getTeacherAssignments(Long classroomId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        return assignmentRepository.findByClassroomIdAndTeacher(classroomId, teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void publishAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        assignment.setIsPublished(true);
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void unpublishAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        assignment.setIsPublished(false);
        assignmentRepository.save(assignment);
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .points(assignment.getPoints())
                .dueDate(assignment.getDueDate())
                .isPublished(assignment.getIsPublished())
                .classroomId(assignment.getClassroomId())
                .createdBy(assignment.getCreatedBy())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
