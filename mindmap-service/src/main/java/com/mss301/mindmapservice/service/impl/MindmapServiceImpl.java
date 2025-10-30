package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.request.MindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;
import com.mss301.mindmapservice.dto.response.MindmapEdgeResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapEdge;
import com.mss301.mindmapservice.entity.MindmapNode;
import com.mss301.mindmapservice.entity.MindmapShare;
import com.mss301.mindmapservice.repository.MindmapEdgeRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.repository.MindmapShareRepository;
import com.mss301.mindmapservice.service.AiService;
import com.mss301.mindmapservice.service.MindmapService;
import com.mss301.mindmapservice.service.RagMindmapService;
import com.mss301.mindmapservice.dto.request.RagMindmapRequest;
import com.mss301.mindmapservice.dto.response.RagMindmapResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapServiceImpl implements MindmapService {

    private final MindmapRepository mindmapRepository;
    private final MindmapNodeRepository mindmapNodeRepository;
    private final MindmapEdgeRepository mindmapEdgeRepository;
    private final MindmapShareRepository mindmapShareRepository;
    private final AiService aiService;
    private final RagMindmapService ragMindmapService;

    @Value("${mindmap.max-mindmaps-free-user:1}")
    private int maxMindmapsFreeUser;

    @Value("${mindmap.max-mindmaps-premium-user:50}")
    private int maxMindmapsPremiumUser;

    @Override
    @Transactional
    public MindmapResponse createMindmap(MindmapRequest request, Long userId) {
        log.info("Creating mindmap for user: {}", userId);

        // Check if user can create more mindmaps
        if (!canUserCreateMindmap(userId, false)) { // TODO: Check premium status
            throw new RuntimeException("User has reached the maximum number of mindmaps");
        }

        Mindmap mindmap = new Mindmap();
        mindmap.setTitle(request.getTitle());
        mindmap.setDescription(request.getDescription());
        mindmap.setUserId(userId);
        mindmap.setGrade(request.getGrade());
        mindmap.setSubject(request.getSubject());
        mindmap.setIsPublic(request.getIsPublic());
        mindmap.setIsAiGenerated(false);
        mindmap.setCreatedAt(LocalDateTime.now());
        mindmap.setUpdatedAt(LocalDateTime.now());

        Mindmap savedMindmap = mindmapRepository.save(mindmap);
        log.info("Mindmap created successfully with ID: {}", savedMindmap.getId());

        return mapToResponse(savedMindmap);
    }

    @Override
    public AiGenerateMindmapResponse generateMindmapWithAi(AiGenerateMindmapRequest request, Long userId) {
        log.info("Generating mindmap with AI (RAG) for user: {}", userId);

        // Check if user can create more mindmaps
        if (!canUserCreateMindmap(userId, false)) { // TODO: Check premium status
            throw new RuntimeException("User has reached the maximum number of mindmaps");
        }

        // Convert to RAG request - need to parse grade from String to Integer
        Integer gradeInteger;
        try {
            gradeInteger = Integer.parseInt(request.getGrade());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid grade format: " + request.getGrade());
        }
        
        RagMindmapRequest ragRequest = RagMindmapRequest.builder()
                .topic(request.getTopic())
                .description(request.getDescription())
                .grade(gradeInteger)
                .subject(request.getSubject())
                .aiProvider(request.getAiProvider())
                .aiModel(request.getAiModel())
                .useDocuments(request.getUseDocuments() != null ? request.getUseDocuments() : false)
                .documentId(request.getDocumentId())
                .chapterId(request.getChapterId())
                .lessonId(request.getLessonId())
                .build();

        // Generate mindmap using RAG
        RagMindmapResponse ragResponse = ragMindmapService.generateRagMindmap(ragRequest, userId);
        
        // Check if RAG generation was successful
        if (!"SUCCESS".equals(ragResponse.getStatus())) {
            return AiGenerateMindmapResponse.builder()
                    .status("FAILED")
                    .errorMessage(ragResponse.getErrorMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // Get the saved mindmap from database
        Mindmap savedMindmap = mindmapRepository.findById(ragResponse.getMindmapId())
                .orElseThrow(() -> new RuntimeException("Mindmap not found after RAG generation"));

        // Return AI response with mindmap data
        AiGenerateMindmapResponse aiResponse = new AiGenerateMindmapResponse();
        aiResponse.setMindmapId(savedMindmap.getId());
        aiResponse.setTitle(savedMindmap.getTitle());
        aiResponse.setDescription(savedMindmap.getDescription());
        aiResponse.setAiProvider(request.getAiProvider().name().toLowerCase());
        aiResponse.setAiModel(request.getAiModel());
        aiResponse.setStatus("SUCCESS");
        aiResponse.setNodesGenerated(ragResponse.getNodesGenerated());
        aiResponse.setEdgesGenerated(ragResponse.getEdgesGenerated());
        aiResponse.setCreatedAt(LocalDateTime.now());
        aiResponse.setMindmap(mapToResponse(savedMindmap));
        
        return aiResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public MindmapResponse getMindmapById(Long id, Long userId) {
        log.info("Getting mindmap by ID: {} for user: {}", id, userId);

        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Increment access count
        mindmap.incrementAccessCount();
        mindmapRepository.save(mindmap);

        return mapToResponse(mindmap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MindmapNodeResponse> getMindmapNodes(Long mindmapId, Long userId) {
        log.info("Getting nodes for mindmap: {} by user: {}", mindmapId, userId);

        // Verify mindmap exists and belongs to user
        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(mindmapId, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Get all nodes for this mindmap
        List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapIdOrderByLevelAndOrderIndex(mindmapId);
        
        return nodes.stream()
                .map(this::mapNodeToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MindmapResponse> getUserMindmaps(Long userId) {
        log.info("Getting mindmaps for user: {}", userId);

        List<Mindmap> mindmaps = mindmapRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return mindmaps.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MindmapResponse> getPublicMindmaps(Pageable pageable) {
        log.info("Getting public mindmaps");

        Page<Mindmap> mindmaps = mindmapRepository.findByIsPublicTrue(pageable);
        return mindmaps.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MindmapResponse> searchMindmaps(String keyword, Long userId) {
        log.info("Searching mindmaps with keyword: {} for user: {}", keyword, userId);

        List<Mindmap> mindmaps = mindmapRepository.findByUserIdAndKeyword(userId, keyword);
        return mindmaps.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public MindmapResponse updateMindmap(Long id, MindmapRequest request, Long userId) {
        log.info("Updating mindmap: {} for user: {}", id, userId);

        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        mindmap.setTitle(request.getTitle());
        mindmap.setDescription(request.getDescription());
        mindmap.setGrade(request.getGrade());
        mindmap.setSubject(request.getSubject());
        mindmap.setIsPublic(request.getIsPublic());
        mindmap.setUpdatedAt(LocalDateTime.now());

        Mindmap updatedMindmap = mindmapRepository.save(mindmap);
        log.info("Mindmap updated successfully");

        return mapToResponse(updatedMindmap);
    }

    @Override
    public void deleteMindmap(Long id, Long userId) {
        log.info("Deleting mindmap: {} for user: {}", id, userId);

        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Delete related entities
        List<MindmapEdge> edges = mindmapEdgeRepository.findByMindmapId(id);
        mindmapEdgeRepository.deleteAll(edges);

        List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapId(id);
        mindmapNodeRepository.deleteAll(nodes);

        List<MindmapShare> shares = mindmapShareRepository.findByMindmapId(id);
        mindmapShareRepository.deleteAll(shares);

        mindmapRepository.delete(mindmap);

        log.info("Mindmap deleted successfully");
    }

    @Override
    public String shareMindmap(Long id, Long userId) {
        log.info("Sharing mindmap: {} for user: {}", id, userId);

        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Generate unique share code
        String shareCode = UUID.randomUUID().toString().substring(0, 8);

        MindmapShare share = new MindmapShare();
        share.setMindmapId(id);
        share.setSharedByUserId(userId);
        share.setShareCode(shareCode);
        share.setPermission(MindmapShare.Permission.VIEW);
        share.setIsActive(true);
        share.setCreatedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());

        mindmapShareRepository.save(share);
        log.info("Mindmap shared with code: {}", shareCode);

        return shareCode;
    }

    @Override
    @Transactional(readOnly = true)
    public MindmapResponse getSharedMindmap(String shareCode) {
        log.info("Getting shared mindmap with code: {}", shareCode);

        MindmapShare share = mindmapShareRepository
                .findActiveShareByCode(shareCode, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired share code"));

        Mindmap mindmap = mindmapRepository
                .findById(share.getMindmapId())
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Increment access count
        mindmap.incrementAccessCount();
        mindmapRepository.save(mindmap);

        return mapToResponse(mindmap);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserMindmapStats(Long userId) {
        log.info("Getting mindmap stats for user: {}", userId);

        Long totalMindmaps = mindmapRepository.countByUserId(userId);
        Long totalAccessCount = mindmapRepository.findByUserId(userId).stream()
                .mapToLong(Mindmap::getAccessCount)
                .sum();
        Long totalFavoriteCount = mindmapRepository.findByUserId(userId).stream()
                .mapToLong(Mindmap::getFavoriteCount)
                .sum();

        return new MindmapStats(totalMindmaps, totalAccessCount, totalFavoriteCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserCreateMindmap(Long userId, boolean isPremium) {
        Long currentCount = mindmapRepository.countByUserId(userId);
        int maxAllowed = isPremium ? maxMindmapsPremiumUser : maxMindmapsFreeUser;

        return currentCount < maxAllowed;
    }

    private MindmapResponse mapToResponse(Mindmap mindmap) {
        // Load nodes and edges for the mindmap
        List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapId(mindmap.getId());
        List<MindmapEdge> edges = mindmapEdgeRepository.findByMindmapId(mindmap.getId());
        
        return MindmapResponse.builder()
                .id(mindmap.getId())
                .title(mindmap.getTitle())
                .description(mindmap.getDescription())
                .userId(mindmap.getUserId())
                .grade(mindmap.getGrade())
                .subject(mindmap.getSubject())
                .isPublic(mindmap.getIsPublic())
                .isAiGenerated(mindmap.getIsAiGenerated())
                .aiProvider(mindmap.getAiProvider())
                .aiModel(mindmap.getAiModel())
                .createdAt(mindmap.getCreatedAt())
                .updatedAt(mindmap.getUpdatedAt())
                .lastAccessedAt(mindmap.getLastAccessedAt())
                .accessCount(mindmap.getAccessCount())
                .favoriteCount(mindmap.getFavoriteCount())
                .shareCount(mindmap.getShareCount())
                .color(mindmap.getColor())
                .difficulty(mindmap.getDifficulty())
                .cognitiveLevel(mindmap.getCognitiveLevel())
                .estimatedTime(mindmap.getEstimatedTime())
                .thumbnailUrl(mindmap.getThumbnailUrl())
                .tags(mindmap.getTags())
                .nodes(nodes.stream().map(this::mapNodeToResponse).collect(Collectors.toList()))
                .edges(edges.stream().map(this::mapEdgeToResponse).collect(Collectors.toList()))
                .build();
    }

    private MindmapNodeResponse mapNodeToResponse(MindmapNode node) {
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

    private MindmapEdgeResponse mapEdgeToResponse(MindmapEdge edge) {
        return MindmapEdgeResponse.builder()
                .id(edge.getId())
                .mindmapId(edge.getMindmapId())
                .fromNodeId(edge.getFromNodeId())
                .toNodeId(edge.getToNodeId())
                .relationshipType(edge.getRelationshipType())
                .label(edge.getLabel())
                .color(edge.getColor())
                .thickness(edge.getThickness())
                .style(edge.getStyle())
                .isDirected(edge.getIsDirected())
                .weight(edge.getWeight())
                .createdAt(edge.getCreatedAt())
                .updatedAt(edge.getUpdatedAt())
                .build();
    }

    private static class MindmapStats {
        public final Long totalMindmaps;
        public final Long totalAccessCount;
        public final Long totalFavoriteCount;

        public MindmapStats(Long totalMindmaps, Long totalAccessCount, Long totalFavoriteCount) {
            this.totalMindmaps = totalMindmaps;
            this.totalAccessCount = totalAccessCount;
            this.totalFavoriteCount = totalFavoriteCount;
        }
    }
}
