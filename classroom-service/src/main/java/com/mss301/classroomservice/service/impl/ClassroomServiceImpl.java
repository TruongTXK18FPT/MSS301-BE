package com.mss301.classroomservice.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
import com.mss301.classroomservice.dto.response.StudentResponse;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.entity.ClassroomMember;
import com.mss301.classroomservice.entity.ClassroomMember.Role;
import com.mss301.classroomservice.repository.ClassroomMemberRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.service.ClassroomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, Long ownerId) {
        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .password(request.getPassword())
                .maxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 50)
                .ownerId(ownerId)
                .build();
        classroom = classroomRepository.save(classroom);

        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(ownerId)
                .role(Role.TEACHER)
                .build());

        return toResponse(classroom);
    }

    @Override
    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request, Long ownerId) {
        Classroom classroom =
                classroomRepository.findById(id).orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        classroom.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        classroom.setPassword(request.getPassword());
        classroom.setMaxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : classroom.getMaxStudents());
        return toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        Classroom classroom =
                classroomRepository.findById(id).orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroomRepository.delete(classroom);
    }

    @Override
    public ClassroomResponse getById(Long id, Long userId) {
        Classroom classroom =
                classroomRepository.findById(id).orElseThrow(() -> new RuntimeException("Classroom not found"));
        // Simple access control: owner or public or member
        boolean isMember =
                classroomMemberRepository.findByClassroomIdAndUserId(id, userId).isPresent();
        if (!classroom.getOwnerId().equals(userId) && !Boolean.TRUE.equals(classroom.getIsPublic()) && !isMember) {
            throw new RuntimeException("Forbidden");
        }
        return toResponse(classroom);
    }

    @Override
    public List<ClassroomResponse> getMyClassrooms(Long ownerId) {
        return classroomRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomResponse> getPublicClassrooms() {
        return classroomRepository.findByIsPublicTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String generateJoinCode(Long id, Long ownerId) {
        Classroom classroom =
                classroomRepository.findById(id).orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        byte[] buf = new byte[6];
        new SecureRandom().nextBytes(buf);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        classroom.setJoinCode(code);
        classroomRepository.save(classroom);
        return code;
    }

    @Override
    @Transactional
    public ClassroomResponse joinByCode(String joinCode, Long userId) {
        Classroom classroom =
                classroomRepository.findByJoinCode(joinCode).orElseThrow(() -> new RuntimeException("Invalid code"));
        classroomMemberRepository
                .findByClassroomIdAndUserId(classroom.getId(), userId)
                .ifPresent(cm -> {
                    throw new RuntimeException("Already joined");
                });
        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(userId)
                .role(Role.STUDENT)
                .build());
        return toResponse(classroom);
    }

    @Override
    public List<ClassroomResponse> searchClassrooms(String keyword) {
        return classroomRepository.findByIsPublicTrueAndNameContainingIgnoreCase(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomResponse joinClassroom(String classroomCode, String password, Long userId) {
        Classroom classroom = classroomRepository.findByJoinCode(classroomCode)
                .orElseThrow(() -> new RuntimeException("Invalid classroom code"));
        
        // Check password if classroom has one
        if (classroom.getPassword() != null && !classroom.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        // Check if already a member
        classroomMemberRepository.findByClassroomIdAndUserId(classroom.getId(), userId)
                .ifPresent(cm -> {
                    throw new RuntimeException("Already joined");
                });
        
        // Add as student
        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(userId)
                .role(Role.STUDENT)
                .build());
        
        return toResponse(classroom);
    }

    @Override
    public List<StudentResponse> getClassroomStudents(Long classroomId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }
        
        return classroomMemberRepository.findByClassroomIdAndRole(classroomId, Role.STUDENT).stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeStudentFromClassroom(Long classroomId, Long studentId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }
        
        ClassroomMember member = classroomMemberRepository.findByClassroomIdAndUserId(classroomId, studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        classroomMemberRepository.delete(member);
    }

    private ClassroomResponse toResponse(Classroom classroom) {
        // Count current students
        long currentStudents = classroomMemberRepository.countByClassroomId(classroom.getId());
        
        return ClassroomResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .isPublic(classroom.getIsPublic())
                .joinCode(classroom.getJoinCode())
                .password(classroom.getPassword())
                .maxStudents(classroom.getMaxStudents())
                .currentStudents((int) currentStudents)
                .ownerId(classroom.getOwnerId())
                .createdAt(classroom.getCreatedAt())
                .updatedAt(classroom.getUpdatedAt())
                .build();
    }

    private StudentResponse toStudentResponse(ClassroomMember member) {
        // In a real implementation, you would fetch user details from user service
        return StudentResponse.builder()
                .userId(member.getUserId())
                .username("User" + member.getUserId()) // Placeholder
                .email("user" + member.getUserId() + "@example.com") // Placeholder
                .fullName("Student " + member.getUserId()) // Placeholder
                .joinedAt(member.getJoinedAt())
                .role(member.getRole().name())
                .build();
    }
}
