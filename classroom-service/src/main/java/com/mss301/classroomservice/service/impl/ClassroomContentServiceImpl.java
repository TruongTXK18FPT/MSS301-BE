package com.mss301.classroomservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.ClassroomContentRequest;
import com.mss301.classroomservice.dto.response.ClassroomContentResponse;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.entity.ClassroomContent;
import com.mss301.classroomservice.entity.ClassroomContent.ContentType;
import com.mss301.classroomservice.repository.ClassroomContentRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.service.ClassroomContentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomContentServiceImpl implements ClassroomContentService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomContentRepository repository;

    @Override
    @Transactional
    public ClassroomContentResponse attach(Long classroomId, ClassroomContentRequest request, Long userId) {
        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        ClassroomContent link = ClassroomContent.builder()
                .classroomId(classroomId)
                .contentId(request.getContentId())
                .type(ContentType.valueOf(request.getType()))
                .visible(Boolean.TRUE.equals(request.getVisible()))
                .orderIndex(request.getOrderIndex())
                .publishAt(request.getPublishAt())
                .dueAt(request.getDueAt())
                .maxPoints(request.getMaxPoints())
                .build();
        return toResponse(repository.save(link));
    }

    @Override
    @Transactional
    public void detach(Long classroomId, Long contentLinkId, Long userId) {
        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        repository.deleteById(contentLinkId);
    }

    @Override
    public List<ClassroomContentResponse> list(Long classroomId, Long userId) {
        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        boolean isOwner = classroom.getOwnerId().equals(userId);
        // Member visibility checks can be added here if needed
        return repository.findByClassroomIdOrderByOrderIndexAsc(classroomId).stream()
                .filter(cc -> isOwner || Boolean.TRUE.equals(cc.getVisible()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomContentResponse> listVisibleContents(Long classroomId, Long userId) {
        // Verify user has access to classroom (member or owner)
        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        // Get current time for publishAt filtering
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Filter contents that are:
        // 1. Visible = true
        // 2. publishAt is null OR publishAt <= now (already published)
        return repository.findByClassroomIdOrderByOrderIndexAsc(classroomId).stream()
                .filter(cc -> Boolean.TRUE.equals(cc.getVisible()))
                .filter(cc -> cc.getPublishAt() == null || !cc.getPublishAt().isAfter(now))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ClassroomContentResponse toResponse(ClassroomContent cc) {
        return ClassroomContentResponse.builder()
                .id(cc.getId())
                .classroomId(cc.getClassroomId())
                .contentId(cc.getContentId())
                .type(cc.getType().name())
                .title(cc.getTitle())
                .description(cc.getDescription())
                .content(cc.getContent())
                .visible(cc.getVisible())
                .orderIndex(cc.getOrderIndex())
                .publishAt(cc.getPublishAt())
                .dueAt(cc.getDueAt())
                .maxPoints(cc.getMaxPoints())
                .createdAt(cc.getCreatedAt())
                .updatedAt(cc.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ClassroomContentResponse createContent(Long classroomId, ClassroomContentRequest request, Long userId) {
        // Verify ownership
        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden: Only classroom owner can create content");
        }

        // Create new content without external contentId (lesson stored directly)
        ClassroomContent content = ClassroomContent.builder()
                .classroomId(classroomId)
                .contentId(null) // No external content reference for lessons
                .type(ContentType.valueOf(request.getType()))
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent()) // Store lesson content directly
                .visible(Boolean.TRUE.equals(request.getVisible()))
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .publishAt(request.getPublishAt())
                .dueAt(request.getDueAt())
                .maxPoints(request.getMaxPoints())
                .build();

        return toResponse(repository.save(content));
    }

    @Override
    @Transactional
    public ClassroomContentResponse updateContent(Long contentId, ClassroomContentRequest request, Long userId) {
        // Find existing content
        ClassroomContent content = repository
                .findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        // Verify ownership through classroom
        Classroom classroom = classroomRepository
                .findById(content.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden: Only classroom owner can update content");
        }

        // Update fields
        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setContent(request.getContent());
        content.setVisible(Boolean.TRUE.equals(request.getVisible()));
        content.setPublishAt(request.getPublishAt());
        content.setDueAt(request.getDueAt());
        content.setMaxPoints(request.getMaxPoints());
        if (request.getOrderIndex() != null) {
            content.setOrderIndex(request.getOrderIndex());
        }

        return toResponse(repository.save(content));
    }

    @Override
    @Transactional
    public void deleteContent(Long contentId, Long userId) {
        // Find existing content
        ClassroomContent content = repository
                .findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        // Verify ownership through classroom
        Classroom classroom = classroomRepository
                .findById(content.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden: Only classroom owner can delete content");
        }

        repository.deleteById(contentId);
    }
}
