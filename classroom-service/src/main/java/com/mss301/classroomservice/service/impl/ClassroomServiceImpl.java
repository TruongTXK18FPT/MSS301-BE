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
import com.mss301.classroomservice.repository.AssignmentRepository;
import com.mss301.classroomservice.repository.ClassroomContentRepository;
import com.mss301.classroomservice.repository.ClassroomMemberRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.QuizRepository;
import com.mss301.classroomservice.service.ClassroomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final ClassroomContentRepository classroomContentRepository;

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, Long ownerId) {
        // Generate joinCode if not provided
        String joinCode = request.getJoinCode();
        if (joinCode == null || joinCode.trim().isEmpty()) {
            joinCode = generateJoinCode();
            System.out.println("Auto-generated joinCode: " + joinCode);
        }
        
        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .password(request.getPassword())
                .joinCode(joinCode)
                .maxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 50)
                .ownerId(ownerId)
                .build();
        classroom = classroomRepository.save(classroom);

        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(ownerId)
                .role(Role.TEACHER)
                .build());

        System.out.println("Created classroom: " + classroom.getName() + " with joinCode: " + classroom.getJoinCode());
        return toResponse(classroom);
    }
    
    // Helper method to generate random join code
    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // Check if code already exists, regenerate if needed
        String generatedCode = code.toString();
        while (classroomRepository.findByJoinCodeIgnoreCase(generatedCode).isPresent()) {
            code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            generatedCode = code.toString();
        }
        
        return generatedCode;
    }

    @Override
    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request, Long ownerId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        classroom.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        classroom.setPassword(request.getPassword());
        classroom.setJoinCode(request.getJoinCode());
        classroom.setMaxStudents(
                request.getMaxStudents() != null ? request.getMaxStudents() : classroom.getMaxStudents());
        return toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroomRepository.delete(classroom);
    }

    @Override
    public ClassroomResponse getById(Long id, Long userId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        // Simple access control: owner or public or member
        boolean isMember = classroomMemberRepository.findByClassroomIdAndUserId(id, userId).isPresent();
        if (!classroom.getOwnerId().equals(userId) && !Boolean.TRUE.equals(classroom.getIsPublic()) && !isMember) {
            throw new RuntimeException("Forbidden");
        }
        return toResponse(classroom);
    }

    @Override
    public List<ClassroomResponse> getMyClassrooms(Long userId) {
        // Get classrooms where user is owner
        List<Classroom> ownedClassrooms = classroomRepository.findByOwnerId(userId);
        
        // Get classrooms where user is a member (student)
        List<Long> memberClassroomIds = classroomMemberRepository.findByUserId(userId).stream()
                .map(ClassroomMember::getClassroomId)
                .collect(Collectors.toList());
        
        List<Classroom> memberClassrooms = classroomRepository.findAllById(memberClassroomIds);
        
        // Combine both lists and remove duplicates
        List<Classroom> allClassrooms = new java.util.ArrayList<>(ownedClassrooms);
        for (Classroom classroom : memberClassrooms) {
            if (!allClassrooms.contains(classroom)) {
                allClassrooms.add(classroom);
            }
        }
        
        return allClassrooms.stream()
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
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
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
        Classroom classroom = classroomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Invalid code"));
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
        // Log for debugging
        System.out.println("Attempting to join classroom with code: " + classroomCode);
        
        // Use case-insensitive search
        Classroom classroom = classroomRepository.findByJoinCodeIgnoreCase(classroomCode)
                .orElseThrow(() -> {
                    System.out.println("No classroom found with joinCode: " + classroomCode);
                    return new RuntimeException("Invalid classroom code: " + classroomCode);
                });

        System.out.println("Found classroom: " + classroom.getName() + " (ID: " + classroom.getId() + ")");

        // Check password if classroom has one
        if (classroom.getPassword() != null && !classroom.getPassword().isEmpty() 
            && !classroom.getPassword().equals(password)) {
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

        System.out.println("User " + userId + " successfully joined classroom " + classroom.getId());
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
        
        // Count assignments
        int assignmentCount = assignmentRepository.findByClassroomId(classroom.getId()).size();
        
        // Count quizzes
        int quizCount = quizRepository.findByClassroomId(classroom.getId()).size();
        
        // Count content items (lessons/mindmaps)
        int contentCount = classroomContentRepository.findByClassroomIdOrderByOrderIndexAsc(classroom.getId()).size();

        return ClassroomResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .isPublic(classroom.getIsPublic())
                .joinCode(classroom.getJoinCode())
                .password(classroom.getPassword())
                .maxStudents(classroom.getMaxStudents())
                .currentStudents((int) currentStudents)
                .assignmentCount(assignmentCount)
                .quizCount(quizCount)
                .contentCount(contentCount)
                .ownerId(classroom.getOwnerId())
                .createdAt(classroom.getCreatedAt())
                .updatedAt(classroom.getUpdatedAt())
                .build();
    }

    private StudentResponse toStudentResponse(ClassroomMember member) {
        // In a real implementation, you would fetch user details from user service
        return StudentResponse.builder()
                .userId(member.getUserId())
                .email("user" + member.getUserId() + "@example.com") // Placeholder
                .fullName("Student " + member.getUserId()) // Placeholder
                .joinedAt(member.getJoinedAt())
                .role(member.getRole().name())
                .build();
    }
}
