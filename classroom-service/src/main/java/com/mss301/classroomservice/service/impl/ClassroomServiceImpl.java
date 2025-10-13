package com.mss301.classroomservice.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
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

    private ClassroomResponse toResponse(Classroom classroom) {
        return ClassroomResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .isPublic(classroom.getIsPublic())
                .joinCode(classroom.getJoinCode())
                .ownerId(classroom.getOwnerId())
                .createdAt(classroom.getCreatedAt())
                .updatedAt(classroom.getUpdatedAt())
                .build();
    }
}
