package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.request.MindmapNodeRequest;
import com.mss301.mindmapservice.dto.request.MindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;
import com.mss301.mindmapservice.dto.response.MindmapEdgeResponse;
import com.mss301.mindmapservice.entity.Concept;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapEdge;
import com.mss301.mindmapservice.entity.MindmapNode;
import com.mss301.mindmapservice.entity.MindmapShare;
import com.mss301.mindmapservice.repository.MindmapEdgeRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.repository.MindmapShareRepository;
import com.mss301.mindmapservice.repository.ConceptRepository;
import com.mss301.mindmapservice.repository.FormulaRepository;
import com.mss301.mindmapservice.repository.ExerciseRepository;
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
    private final ConceptRepository conceptRepository;
    private final FormulaRepository formulaRepository;
    private final ExerciseRepository exerciseRepository;
    private final RagMindmapService ragMindmapService;
    private final AiService aiService;

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
        log.info("Generating mindmap with AI for user: {}", userId);

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
        
        // Check if user wants to use documents (RAG) or direct AI generation
        boolean useDocuments = request.getUseDocuments() != null && request.getUseDocuments();
        
        if (useDocuments) {
            // Use RAG service - mindmap based on documents
            log.info("Using RAG service with documents for mindmap generation");
            
            RagMindmapRequest ragRequest = RagMindmapRequest.builder()
                    .topic(request.getTopic())
                    .description(request.getDescription())
                    .grade(gradeInteger)
                    .subject(request.getSubject())
                    .aiProvider(request.getAiProvider())
                    .aiModel(request.getAiModel())
                    .useDocuments(true)
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
            
        } else {
            // Use Direct AI service - mindmap without documents
            log.info("Using Direct AI (Gemini) for mindmap generation without documents");
            
            // Call AiService for direct generation
            AiGenerateMindmapResponse aiResponse = aiService.generateMindmap(request, userId);
            
            // If successful, get mindmap and attach full response
            if ("SUCCESS".equals(aiResponse.getStatus())) {
                Mindmap savedMindmap = mindmapRepository.findById(aiResponse.getMindmapId())
                        .orElseThrow(() -> new RuntimeException("Mindmap not found after AI generation"));
                aiResponse.setMindmap(mapToResponse(savedMindmap));
            }
            
            return aiResponse;
        }
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
    public MindmapResponse viewMindmap(Long id, Long userId) {
        log.info("Viewing mindmap: {} by user: {}", id, userId);

        // Find mindmap by ID first
        Mindmap mindmap = mindmapRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Check if user can view: must be owner OR mindmap must be PUBLIC
        if (!mindmap.getUserId().equals(userId) && mindmap.getVisibility() != Mindmap.Visibility.PUBLIC) {
            throw new RuntimeException("You don't have permission to view this mindmap");
        }

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
    public List<MindmapNodeResponse> viewMindmapNodes(Long mindmapId, Long userId) {
        log.info("Viewing nodes for mindmap: {} by user: {}", mindmapId, userId);

        // Find mindmap by ID first
        Mindmap mindmap = mindmapRepository
                .findById(mindmapId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Check if user can view: must be owner OR mindmap must be PUBLIC
        if (!mindmap.getUserId().equals(userId) && mindmap.getVisibility() != Mindmap.Visibility.PUBLIC) {
            throw new RuntimeException("You don't have permission to view this mindmap");
        }

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
        if (request.getIsPublic() != null) {
            mindmap.setIsPublic(request.getIsPublic());
        }
        if (request.getVisibility() != null) {
            mindmap.setVisibility(request.getVisibility());
            // Sync isPublic with visibility for backward compatibility
            mindmap.setIsPublic(request.getVisibility() == Mindmap.Visibility.PUBLIC);
        }
        mindmap.setUpdatedAt(LocalDateTime.now());

        Mindmap updatedMindmap = mindmapRepository.save(mindmap);
        log.info("Mindmap updated successfully");

        return mapToResponse(updatedMindmap);
    }

    @Override
    @Transactional
    public MindmapResponse updateMindmapWithNodesAndEdges(Long id, MindmapRequest request, Long userId) {
        log.info("Updating mindmap with nodes and edges: {} for user: {}", id, userId);

        // Verify mindmap exists and belongs to user
        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Update mindmap metadata
        if (request.getTitle() != null) mindmap.setTitle(request.getTitle());
        if (request.getDescription() != null) mindmap.setDescription(request.getDescription());
        if (request.getGrade() != null) mindmap.setGrade(request.getGrade());
        if (request.getSubject() != null) mindmap.setSubject(request.getSubject());
        if (request.getIsPublic() != null) {
            mindmap.setIsPublic(request.getIsPublic());
        }
        if (request.getVisibility() != null) {
            mindmap.setVisibility(request.getVisibility());
            // Sync isPublic with visibility for backward compatibility
            mindmap.setIsPublic(request.getVisibility() == Mindmap.Visibility.PUBLIC);
        }
        mindmap.setUpdatedAt(LocalDateTime.now());
        mindmapRepository.save(mindmap);

        // Update nodes if provided
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            log.info("Updating {} nodes", request.getNodes().size());

            // Get existing nodes to check which ones should be kept vs deleted
            List<MindmapNode> existingNodes = mindmapNodeRepository.findByMindmapId(id);
            log.info("Found {} existing nodes in database", existingNodes.size());
            
            // Map of existing node IDs (from request) to keep entities
            Set<Long> requestNodeIds = request.getNodes().stream()
                .map(MindmapNodeRequest::getId)
                .filter(nodeId -> nodeId != null && nodeId > 0)
                .collect(Collectors.toSet());
            
            log.info("Request contains {} nodes with valid IDs: {}", requestNodeIds.size(), requestNodeIds);
            log.info("Request node IDs detail: {}", request.getNodes().stream()
                .map(n -> String.format("[title=%s, id=%s]", n.getTitle(), n.getId()))
                .collect(Collectors.joining(", ")));
            
            // Find nodes that are being REMOVED (not in request) - only delete entities for these
            List<MindmapNode> nodesToDelete = existingNodes.stream()
                .filter(node -> !requestNodeIds.contains(node.getId()))
                .collect(Collectors.toList());
            
            log.info("Nodes to DELETE (not in request): {} nodes - IDs: {}", 
                nodesToDelete.size(),
                nodesToDelete.stream().map(n -> n.getId().toString()).collect(Collectors.joining(", ")));
            
            if (!nodesToDelete.isEmpty()) {
                log.info("Found {} nodes to delete (not in update request)", nodesToDelete.size());
                
                // Step 1: Delete edges for removed nodes
                List<MindmapEdge> edgesToDelete = mindmapEdgeRepository.findByMindmapId(id).stream()
                    .filter(edge -> nodesToDelete.stream().anyMatch(n -> 
                        n.getId().equals(edge.getFromNodeId()) || n.getId().equals(edge.getToNodeId())))
                    .collect(Collectors.toList());
                
                if (!edgesToDelete.isEmpty()) {
                    mindmapEdgeRepository.deleteAll(edgesToDelete);
                    mindmapEdgeRepository.flush();
                    log.info("Deleted {} edges for removed nodes", edgesToDelete.size());
                }
                
                // Step 2: Delete entity data ONLY for removed nodes (preserve entities for kept nodes)
                int deletedConcepts = 0;
                int deletedFormulas = 0;
                int deletedExercises = 0;
                
                for (MindmapNode node : nodesToDelete) {
                    Long nodeId = node.getId();
                    
                    List<Concept> concepts = conceptRepository.findByNodeId(nodeId);
                    if (!concepts.isEmpty()) {
                        conceptRepository.deleteAll(concepts);
                        deletedConcepts += concepts.size();
                    }
                    
                    List<com.mss301.mindmapservice.entity.Formula> formulas = formulaRepository.findByNodeId(nodeId);
                    if (!formulas.isEmpty()) {
                        formulaRepository.deleteAll(formulas);
                        deletedFormulas += formulas.size();
                    }
                    
                    List<com.mss301.mindmapservice.entity.Exercise> exercises = exerciseRepository.findByNodeId(nodeId);
                    if (!exercises.isEmpty()) {
                        exerciseRepository.deleteAll(exercises);
                        deletedExercises += exercises.size();
                    }
                }
                
                conceptRepository.flush();
                formulaRepository.flush();
                exerciseRepository.flush();
                log.info("Deleted entity data for removed nodes: {} concepts, {} formulas, {} exercises", 
                    deletedConcepts, deletedFormulas, deletedExercises);
                
                // Step 3: Clear parent references for nodes to delete
                for (MindmapNode node : nodesToDelete) {
                    node.setParentNodeId(null);
                }
                mindmapNodeRepository.saveAll(nodesToDelete);
                mindmapNodeRepository.flush();
                log.info("Cleared parent references for {} nodes to delete", nodesToDelete.size());

                // Step 4: Delete the removed nodes
                mindmapNodeRepository.deleteAll(nodesToDelete);
                mindmapNodeRepository.flush();
                log.info("Deleted {} removed nodes", nodesToDelete.size());
            } else {
                log.info("No nodes to delete - all existing nodes are being kept/updated");
                
                // Just delete all edges - they will be recreated from parentNodeId
                List<MindmapEdge> existingEdges = mindmapEdgeRepository.findByMindmapId(id);
                if (!existingEdges.isEmpty()) {
                    mindmapEdgeRepository.deleteAll(existingEdges);
                    mindmapEdgeRepository.flush();
                    log.info("Deleted {} existing edges (will be recreated)", existingEdges.size());
                }
            }

            // Update or create nodes - preserve entities for existing nodes
            // Build a map of existing nodes by ID for quick lookup
            Map<Long, MindmapNode> existingNodeMap = existingNodes.stream()
                .collect(Collectors.toMap(MindmapNode::getId, node -> node));
            
            log.info("Existing node map has {} entries", existingNodeMap.size());
            
            List<MindmapNode> savedNodes = new ArrayList<>();
            int updateCount = 0;
            int createCount = 0;

            for (MindmapNodeRequest nodeRequest : request.getNodes()) {
                try {
                    MindmapNode node;
                    
                    // Check if this is an update (node has ID and exists) or new node
                    if (nodeRequest.getId() != null && nodeRequest.getId() > 0 && 
                        existingNodeMap.containsKey(nodeRequest.getId())) {
                        // UPDATE existing node - preserve its ID and created date
                        node = existingNodeMap.get(nodeRequest.getId());
                        log.info("✓ UPDATING existing node ID: {} - '{}' (entities will be PRESERVED)", 
                            node.getId(), node.getTitle());
                        updateCount++;
                    } else {
                        // CREATE new node
                        node = new MindmapNode();
                        node.setMindmapId(id);
                        node.setCreatedAt(LocalDateTime.now());
                        log.info("✓ CREATING new node: '{}' (requestId={}, no entities yet)", 
                            nodeRequest.getTitle(), nodeRequest.getId());
                        createCount++;
                    }
                    
                    // Update node properties (for both new and existing nodes)
                    String title = nodeRequest.getTitle();
                    if (title == null || title.trim().isEmpty()) {
                        log.warn("Node title is empty, using default: {}", nodeRequest);
                        title = "Untitled";
                    }
                    node.setTitle(title.trim());
                    node.setContent(nodeRequest.getContent());
                    
                    // Convert nodeType
                    MindmapNode.NodeType nodeType = nodeRequest.getNodeType();
                    if (nodeType == null) {
                        nodeType = MindmapNode.NodeType.CONCEPT;
                        log.debug("NodeType is null for node '{}', defaulting to CONCEPT", title);
                    }
                    node.setNodeType(nodeType);

                    node.setPositionX(nodeRequest.getPositionX() != null ? nodeRequest.getPositionX() : 0.0);
                    node.setPositionY(nodeRequest.getPositionY() != null ? nodeRequest.getPositionY() : 0.0);
                    node.setWidth(nodeRequest.getWidth());
                    node.setHeight(nodeRequest.getHeight());
                    node.setColor(nodeRequest.getColor());
                    node.setBackgroundColor(nodeRequest.getBackgroundColor());
                    node.setBorderColor(nodeRequest.getBorderColor());
                    node.setFontSize(nodeRequest.getFontSize());
                    node.setFontFamily(nodeRequest.getFontFamily());
                    node.setIsBold(nodeRequest.getIsBold() != null ? nodeRequest.getIsBold() : false);
                    node.setIsItalic(nodeRequest.getIsItalic() != null ? nodeRequest.getIsItalic() : false);
                    node.setIsUnderline(nodeRequest.getIsUnderline() != null ? nodeRequest.getIsUnderline() : false);
                    node.setLevel(nodeRequest.getLevel() != null ? nodeRequest.getLevel() : 0);
                    node.setOrderIndex(nodeRequest.getOrderIndex() != null ? nodeRequest.getOrderIndex() : 0);
                    node.setIsCollapsed(nodeRequest.getIsCollapsed() != null ? nodeRequest.getIsCollapsed() : false);
                    node.setUpdatedAt(LocalDateTime.now());
                    // Don't set parentNodeId yet

                    MindmapNode savedNode = mindmapNodeRepository.save(node);
                    savedNodes.add(savedNode);

                    log.debug("Saved node: {} with ID: {}, type: {} (entities preserved)", 
                        savedNode.getTitle(), savedNode.getId(), savedNode.getNodeType());
                } catch (Exception e) {
                    log.error("Failed to save node: {} - Error: {}", nodeRequest.getTitle(), e.getMessage(), e);
                }
            }
            
            log.info("=== NODE SAVE SUMMARY: Updated={}, Created={}, Total saved={} ===", 
                updateCount, createCount, savedNodes.size());

            // Second pass: update parent relationships based on node hierarchy (level)
            // Don't use parentNodeId from request - those are old IDs that were deleted
            // Instead, set parent based on node level:
            // - Level 0 (root/central): no parent
            // - Level 1 (branches): parent is level 0 node
            // - Level 2+ (sub-branches): parent is closest level 1 node by position
            
            MindmapNode rootNode = savedNodes.stream()
                .filter(n -> n.getLevel() == 0)
                .findFirst()
                .orElse(null);
            
            if (rootNode != null) {
                List<MindmapNode> level1Nodes = savedNodes.stream()
                    .filter(n -> n.getLevel() == 1)
                    .collect(Collectors.toList());
                
                // Set parent for level 1 nodes (branches connect to root)
                for (MindmapNode level1Node : level1Nodes) {
                    level1Node.setParentNodeId(rootNode.getId());
                }
                
                // Set parent for level 2+ nodes (connect to nearest level 1 by position)
                List<MindmapNode> level2PlusNodes = savedNodes.stream()
                    .filter(n -> n.getLevel() >= 2)
                    .collect(Collectors.toList());
                
                for (MindmapNode node : level2PlusNodes) {
                    // Find closest level 1 node by Euclidean distance
                    MindmapNode closestParent = findClosestLevel1Node(node, level1Nodes);
                    if (closestParent != null) {
                        node.setParentNodeId(closestParent.getId());
                    }
                }
                
                // Save all parent relationships in one batch
                mindmapNodeRepository.saveAll(savedNodes);
                mindmapNodeRepository.flush(); // Force immediate update
                log.info("Updated parent relationships for {} nodes", savedNodes.size());
            }
            
            if (savedNodes.size() < request.getNodes().size()) {
                log.warn("Only saved {} out of {} nodes. Some nodes may have failed validation or encountered errors.", 
                        savedNodes.size(), request.getNodes().size());
            }

            log.info("Created and linked {} new nodes", savedNodes.size());
        }

        // Update edges if provided
        if (request.getEdges() != null && !request.getEdges().isEmpty()) {
            log.info("Updating {} edges", request.getEdges().size());

            // Note: Edges may have been deleted already during node deletion above
            // This check ensures we handle cases where nodes weren't updated but edges need updating
            List<MindmapEdge> existingEdges = mindmapEdgeRepository.findByMindmapId(id);
            if (!existingEdges.isEmpty()) {
                mindmapEdgeRepository.deleteAll(existingEdges);
                mindmapEdgeRepository.flush(); // Force immediate deletion
                log.info("Deleted {} existing edges", existingEdges.size());
            }

            // Create new edges
            List<MindmapEdge> newEdges = request.getEdges().stream()
                .map(edgeRequest -> {
                    MindmapEdge edge = new MindmapEdge();
                    edge.setMindmapId(id);
                    edge.setFromNodeId(edgeRequest.getFromNodeId());
                    edge.setToNodeId(edgeRequest.getToNodeId());
                    edge.setRelationshipType(
                            edgeRequest.getRelationshipType() != null
                                    ? edgeRequest.getRelationshipType().name()
                                    : null);
                    edge.setLabel(edgeRequest.getLabel());
                    edge.setColor(edgeRequest.getColor());
                    edge.setThickness(edgeRequest.getThickness());
                    edge.setStyle(edgeRequest.getStyle());
                    edge.setIsDirected(edgeRequest.getIsDirected());
                    edge.setWeight(edgeRequest.getWeight());
                    edge.setCreatedAt(LocalDateTime.now());
                    edge.setUpdatedAt(LocalDateTime.now());
                    return edge;
                })
                .collect(Collectors.toList());

            mindmapEdgeRepository.saveAll(newEdges);
            log.info("Created {} new edges", newEdges.size());
        }

        log.info("Mindmap with nodes and edges updated successfully");
        return mapToResponse(mindmap);
    }

    @Override
    @Transactional
    public void deleteMindmap(Long id, Long userId) {
        log.info("Deleting mindmap: {} for user: {}", id, userId);

        Mindmap mindmap = mindmapRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Mindmap not found"));

        // Get all nodes first to delete their related entities
        List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapId(id);
        log.debug("Found {} nodes to delete", nodes.size());

        // Step 1: Delete Concept, Formula, Exercise for all nodes
        // These have foreign key constraints to node_id
        for (MindmapNode node : nodes) {
            try {
                conceptRepository.deleteByNodeId(node.getId());
                formulaRepository.deleteByNodeId(node.getId());
                exerciseRepository.deleteByNodeId(node.getId());
                log.debug("Deleted related entities for node: {}", node.getId());
            } catch (Exception e) {
                log.warn("Error deleting related entities for node {}: {}", node.getId(), e.getMessage());
                // Continue with other nodes
            }
        }

        // Step 2: Delete edges (they reference nodes via fromNodeId/toNodeId)
        List<MindmapEdge> edges = mindmapEdgeRepository.findByMindmapId(id);
        if (!edges.isEmpty()) {
            mindmapEdgeRepository.deleteAll(edges);
            mindmapEdgeRepository.flush(); // Force immediate deletion
            log.debug("Deleted {} edges", edges.size());
        }

        // Step 3: Clear parent relationships in nodes before deletion
        for (MindmapNode node : nodes) {
            node.setParentNodeId(null);
        }
        mindmapNodeRepository.saveAll(nodes);
        mindmapNodeRepository.flush(); // Force immediate update to database

        // Step 4: Delete nodes (now safe since Concept/Formula/Exercise are deleted)
        if (!nodes.isEmpty()) {
            mindmapNodeRepository.deleteAll(nodes);
            mindmapNodeRepository.flush(); // Force immediate deletion
            log.debug("Deleted {} nodes", nodes.size());
        }

        // Step 5: Delete shares
        List<MindmapShare> shares = mindmapShareRepository.findByMindmapId(id);
        if (!shares.isEmpty()) {
            mindmapShareRepository.deleteAll(shares);
            log.debug("Deleted {} shares", shares.size());
        }

        // Step 6: Finally delete the mindmap itself
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
                .visibility(mindmap.getVisibility())
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

    /**
     * Find closest level 1 node to a given node by Euclidean distance
     */
    private MindmapNode findClosestLevel1Node(MindmapNode targetNode, List<MindmapNode> level1Nodes) {
        if (level1Nodes.isEmpty()) return null;
        
        MindmapNode closest = level1Nodes.get(0);
        double minDistance = calculateDistance(targetNode, closest);
        
        for (MindmapNode level1Node : level1Nodes) {
            double distance = calculateDistance(targetNode, level1Node);
            if (distance < minDistance) {
                minDistance = distance;
                closest = level1Node;
            }
        }
        
        return closest;
    }

    /**
     * Calculate Euclidean distance between two nodes
     */
    private double calculateDistance(MindmapNode node1, MindmapNode node2) {
        double dx = node1.getPositionX() - node2.getPositionX();
        double dy = node1.getPositionY() - node2.getPositionY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
