package com.mss301.mindmapservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.MindmapContentLinkRequest;
import com.mss301.mindmapservice.dto.response.MindmapContentLinkResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapContentLink;
import com.mss301.mindmapservice.repository.MindmapContentLinkRepository;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.service.MindmapContentLinkService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MindmapContentLinkServiceImpl implements MindmapContentLinkService {

    private final MindmapRepository mindmapRepository;
    private final MindmapContentLinkRepository repository;

    @Override
    @Transactional
    public MindmapContentLinkResponse attach(Long mindmapId, MindmapContentLinkRequest request, Long userId) {
        Mindmap mindmap =
                mindmapRepository.findById(mindmapId).orElseThrow(() -> new RuntimeException("Mindmap not found"));
        if (!mindmap.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        MindmapContentLink link = MindmapContentLink.builder()
                .mindmapId(mindmapId)
                .contentId(request.getContentId())
                .contentType(request.getContentType())
                .note(request.getNote())
                .build();
        return toResponse(repository.save(link));
    }

    @Override
    @Transactional
    public void detach(Long mindmapId, Long linkId, Long userId) {
        Mindmap mindmap =
                mindmapRepository.findById(mindmapId).orElseThrow(() -> new RuntimeException("Mindmap not found"));
        if (!mindmap.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        repository.deleteById(linkId);
    }

    @Override
    public List<MindmapContentLinkResponse> list(Long mindmapId, Long userId) {
        Mindmap mindmap =
                mindmapRepository.findById(mindmapId).orElseThrow(() -> new RuntimeException("Mindmap not found"));
        if (!mindmap.getUserId().equals(userId) && !Boolean.TRUE.equals(mindmap.getIsPublic())) {
            throw new RuntimeException("Forbidden");
        }
        return repository.findByMindmapId(mindmapId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MindmapContentLinkResponse toResponse(MindmapContentLink link) {
        return MindmapContentLinkResponse.builder()
                .id(link.getId())
                .mindmapId(link.getMindmapId())
                .contentId(link.getContentId())
                .contentType(link.getContentType())
                .note(link.getNote())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
