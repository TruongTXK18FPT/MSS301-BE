package com.mss301.contentservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.request.ContentItemRequest;
import com.mss301.contentservice.dto.response.ContentItemResponse;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.service.ContentItemService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentItemServiceImpl implements ContentItemService {

    private final ContentItemRepository repository;

    @Override
    @Transactional
    public ContentItemResponse create(ContentItemRequest request, Long ownerId) {
        ContentItem item = ContentItem.builder()
                .ownerId(ownerId)
                .type(Type.valueOf(request.getType()))
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .subject(request.getSubject())
                .grade(request.getGrade())
                .tags(request.getTags())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();
        return toResponse(repository.save(item));
    }

    @Override
    @Transactional
    public ContentItemResponse update(Long id, ContentItemRequest request, Long ownerId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        item.setType(Type.valueOf(request.getType()));
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setContent(request.getContent());
        item.setSubject(request.getSubject());
        item.setGrade(request.getGrade());
        item.setTags(request.getTags());
        item.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        return toResponse(repository.save(item));
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        repository.delete(item);
    }

    @Override
    public ContentItemResponse getById(Long id, Long userId) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId) && !Boolean.TRUE.equals(item.getIsPublic())) {
            throw new RuntimeException("Forbidden");
        }
        return toResponse(item);
    }

    @Override
    public List<ContentItemResponse> getMyContents(Long ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ContentItemResponse> getPublicContents() {
        return repository.findByIsPublicTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ContentItemResponse> searchPublic(String subject, String grade, String keyword) {
        return repository.searchPublic(subject, grade, keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ContentItemResponse toResponse(ContentItem item) {
        return ContentItemResponse.builder()
                .id(item.getId())
                .ownerId(item.getOwnerId())
                .type(item.getType().name())
                .title(item.getTitle())
                .description(item.getDescription())
                .content(item.getContent())
                .subject(item.getSubject())
                .grade(item.getGrade())
                .tags(item.getTags())
                .isPublic(item.getIsPublic())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
