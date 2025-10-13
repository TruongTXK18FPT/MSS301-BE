package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.MindmapNodeRequest;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapNode;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.service.MindmapNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapNodeServiceImpl implements MindmapNodeService {

    private final MindmapNodeRepository mindmapNodeRepository;
    private final MindmapRepository mindmapRepository;

    @Override
    @Transactional
    public MindmapNodeResponse createNode(MindmapNodeRequest request, Long mindmapId, Long userId) {
        log.info("Creating node for mindmap: {} by user: {}", mindmapId, userId);

        // Verify mindmap ownership
        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmapId);
        node.setTitle(request.getTitle());
        node.setContent(request.getContent());
        node.setNodeType(request.getNodeType());
        node.setPositionX(request.getPositionX());
        node.setPositionY(request.getPositionY());
        node.setWidth(request.getWidth());
        node.setHeight(request.getHeight());
        node.setColor(request.getColor());
        node.setBackgroundColor(request.getBackgroundColor());
        node.setBorderColor(request.getBorderColor());
        node.setFontSize(request.getFontSize());
        node.setFontFamily(request.getFontFamily());
        node.setIsBold(request.getIsBold());
        node.setIsItalic(request.getIsItalic());
        node.setIsUnderline(request.getIsUnderline());
        node.setParentNodeId(request.getParentNodeId());
        node.setLevel(request.getLevel());
        node.setOrderIndex(request.getOrderIndex());
        node.setIsCollapsed(request.getIsCollapsed());
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        MindmapNode savedNode = mindmapNodeRepository.save(node);
        log.info("Node created successfully with ID: {}", savedNode.getId());

        return mapToResponse(savedNode);
    }

    @Override
    @Transactional(readOnly = true)
    public MindmapNodeResponse getNodeById(Long id, Long mindmapId, Long userId) {
        log.info("Getting node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = mindmapNodeRepository
                .findByIdAndMindmapId(id, mindmapId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        return mapToResponse(node);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MindmapNodeResponse> getNodesByMindmapId(Long mindmapId, Long userId) {
        log.info("Getting nodes for mindmap: {} by user: {}", mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapIdOrderByLevelAndOrderIndex(mindmapId);
        return nodes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public MindmapNodeResponse updateNode(Long id, MindmapNodeRequest request, Long mindmapId, Long userId) {
        log.info("Updating node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = mindmapNodeRepository
                .findByIdAndMindmapId(id, mindmapId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        node.setTitle(request.getTitle());
        node.setContent(request.getContent());
        node.setNodeType(request.getNodeType());
        node.setPositionX(request.getPositionX());
        node.setPositionY(request.getPositionY());
        node.setWidth(request.getWidth());
        node.setHeight(request.getHeight());
        node.setColor(request.getColor());
        node.setBackgroundColor(request.getBackgroundColor());
        node.setBorderColor(request.getBorderColor());
        node.setFontSize(request.getFontSize());
        node.setFontFamily(request.getFontFamily());
        node.setIsBold(request.getIsBold());
        node.setIsItalic(request.getIsItalic());
        node.setIsUnderline(request.getIsUnderline());
        node.setParentNodeId(request.getParentNodeId());
        node.setLevel(request.getLevel());
        node.setOrderIndex(request.getOrderIndex());
        node.setIsCollapsed(request.getIsCollapsed());
        node.setUpdatedAt(LocalDateTime.now());

        MindmapNode updatedNode = mindmapNodeRepository.save(node);
        log.info("Node updated successfully");

        return mapToResponse(updatedNode);
    }

    @Override
    public void deleteNode(Long id, Long mindmapId, Long userId) {
        log.info("Deleting node: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = mindmapNodeRepository
                .findByIdAndMindmapId(id, mindmapId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        mindmapNodeRepository.delete(node);
        log.info("Node deleted successfully");
    }

    @Override
    public MindmapNodeResponse moveNode(Long id, Double newX, Double newY, Long mindmapId, Long userId) {
        log.info("Moving node: {} to position ({}, {}) for mindmap: {} by user: {}", id, newX, newY, mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = mindmapNodeRepository
                .findByIdAndMindmapId(id, mindmapId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        node.setPositionX(newX);
        node.setPositionY(newY);
        node.setUpdatedAt(LocalDateTime.now());

        MindmapNode updatedNode = mindmapNodeRepository.save(node);
        log.info("Node moved successfully");

        return mapToResponse(updatedNode);
    }

    @Override
    public MindmapNodeResponse updateNodeStyle(Long id, MindmapNodeRequest request, Long mindmapId, Long userId) {
        log.info("Updating node style: {} for mindmap: {} by user: {}", id, mindmapId, userId);

        // Verify mindmap ownership
        mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found or access denied"));

        MindmapNode node = mindmapNodeRepository
                .findByIdAndMindmapId(id, mindmapId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        node.setColor(request.getColor());
        node.setBackgroundColor(request.getBackgroundColor());
        node.setBorderColor(request.getBorderColor());
        node.setFontSize(request.getFontSize());
        node.setFontFamily(request.getFontFamily());
        node.setIsBold(request.getIsBold());
        node.setIsItalic(request.getIsItalic());
        node.setIsUnderline(request.getIsUnderline());
        node.setUpdatedAt(LocalDateTime.now());

        MindmapNode updatedNode = mindmapNodeRepository.save(node);
        log.info("Node style updated successfully");

        return mapToResponse(updatedNode);
    }

    private MindmapNodeResponse mapToResponse(MindmapNode node) {
        return MindmapNodeResponse.builder()
                .id(node.getId())
                .mindmapId(node.getMindmapId())
                .title(node.getTitle())
                .content(node.getContent())
                .nodeType(node.getNodeType())
                .positionX(node.getPositionX())
                .positionY(node.getPositionY())
                .width(node.getWidth())
                .height(node.getHeight())
                .color(node.getColor())
                .backgroundColor(node.getBackgroundColor())
                .borderColor(node.getBorderColor())
                .fontSize(node.getFontSize())
                .fontFamily(node.getFontFamily())
                .isBold(node.getIsBold())
                .isItalic(node.getIsItalic())
                .isUnderline(node.getIsUnderline())
                .parentNodeId(node.getParentNodeId())
                .level(node.getLevel())
                .orderIndex(node.getOrderIndex())
                .isCollapsed(node.getIsCollapsed())
                .createdAt(node.getCreatedAt())
                .updatedAt(node.getUpdatedAt())
                .build();
    }
}
