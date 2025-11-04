package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.mss301.mindmapservice.client.RagServiceClient;
import com.mss301.mindmapservice.dto.rag.RagRequest;
import com.mss301.mindmapservice.dto.rag.RagResponse;
import com.mss301.mindmapservice.dto.request.RagMindmapRequest;
import com.mss301.mindmapservice.dto.response.RagMindmapResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapEdge;
import com.mss301.mindmapservice.entity.MindmapNode;
import com.mss301.mindmapservice.service.RagMindmapService;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.repository.MindmapEdgeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagMindmapServiceImpl implements RagMindmapService {

    private final RagServiceClient ragServiceClient;
    private final MindmapRepository mindmapRepository;
    private final MindmapNodeRepository mindmapNodeRepository;
    private final MindmapEdgeRepository mindmapEdgeRepository;

    @Value("${rag.mindmap-generation.max-documents:5}")
    private Integer maxDocuments;

    @Override
    @Transactional
    public RagMindmapResponse generateRagMindmap(RagMindmapRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("Generating RAG mindmap for user: {} with topic: {}", userId, request.getTopic());

        try {
            // Get relevant documents using RAG (optional for general mindmap)
            String ragContext = null;
            if (Boolean.TRUE.equals(request.getUseDocuments())) {
                ragContext = getRelevantDocuments(request);
            }

            // Create mindmap entity
            Mindmap mindmap = createMindmapFromRagRequest(request, userId, ragContext != null ? ragContext : "");
            
            // SAVE mindmap first to get ID
            Mindmap savedMindmap = mindmapRepository.save(mindmap);
            log.info("Saved mindmap with ID: {}", savedMindmap.getId());

            // Call RAG service to generate mindmap structure with AI
            String aiGeneratedJson = callRagServiceForMindmapGeneration(request, ragContext);

            // Parse JSON and generate nodes (WITHOUT parent relationships yet)
            List<MindmapNode> nodes = parseNodesFromAiResponse(savedMindmap, aiGeneratedJson);

            // SAVE nodes FIRST to get their generated IDs
            List<MindmapNode> savedNodes = new ArrayList<>();
            if (!nodes.isEmpty()) {
                savedNodes = mindmapNodeRepository.saveAll(nodes);
                mindmapNodeRepository.flush();  // Ensure nodes are persisted before generating edges
                log.info("Saved {} nodes", savedNodes.size());

                // NOW set parent relationships after IDs are generated
                // Re-map parent relationships based on level and position
                setParentRelationships(savedNodes);

                // Save the updated nodes with parent relationships
                mindmapNodeRepository.saveAll(savedNodes);
                mindmapNodeRepository.flush();
                log.info("Updated parent relationships for {} nodes", savedNodes.size());
            } else {
                log.warn("No nodes generated from AI response");
            }

            // THEN generate edges based on saved nodes with their IDs and parent relationships
            List<MindmapEdge> edges = generateEdgesFromNodes(savedMindmap, savedNodes);

            if (!edges.isEmpty()) {
                mindmapEdgeRepository.saveAll(edges);
                mindmapEdgeRepository.flush();  // Ensure edges are persisted
                log.info("Saved {} edges", edges.size());
            }

            long processingTime = System.currentTimeMillis() - startTime;

            return RagMindmapResponse.builder()
                    .mindmapId(savedMindmap.getId())
                    .title(savedMindmap.getTitle())
                    .description(savedMindmap.getDescription())
                    .aiProvider(request.getAiProvider().name().toLowerCase())
                    .aiModel(request.getAiModel())
                    .status("SUCCESS")
                    .nodesGenerated(nodes.size())
                    .edgesGenerated(edges.size())
                    .documentsUsed(ragContext != null ? maxDocuments : 0)
                    .ragContext(ragContext != null ? ragContext : "")
                    .averageRelevanceScore(ragContext != null ? 0.85 : 0.0)
                    .createdAt(LocalDateTime.now())
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate RAG mindmap: {}", e.getMessage(), e);
            return createErrorResponse("Failed to generate mindmap: " + e.getMessage());
        }
    }

    @Override
    public String getRelevantDocuments(RagMindmapRequest request) {
        log.info("Getting relevant documents for topic: {}", request.getTopic());

        try {
            String query = request.getDocumentQuery() != null
                    ? request.getDocumentQuery()
                    : "Create a mindmap about " + request.getTopic() + " for grade " + request.getGrade();

            RagRequest ragRequest = RagRequest.builder()
                    .queryText(query)
                    .mode("CHAT")
                    .llmProvider(request.getAiProvider().name())
                    .useSemantic(true)
                    .topK(maxDocuments)
                    .build();

            RagResponse ragResponse = ragServiceClient.processRagQuery(ragRequest);

            if (ragResponse != null && ragResponse.getResults() != null) {
                return ragResponse.getResults().stream()
                        .map(RagResponse.RagResult::getContent)
                        .collect(Collectors.joining("\n\n"));
            }

            return null;

        } catch (WebClientResponseException e) {
            log.error("RAG service error: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to get relevant documents: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isRagServiceAvailable() {
        try {
            RagRequest testRequest = RagRequest.builder()
                    .queryText("test")
                    .mode("CHAT")
                    .llmProvider("gemini")
                    .build();

            ragServiceClient.processRagQuery(testRequest);
            return true;
        } catch (Exception e) {
            log.warn("RAG service is not available: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unused")
    private Mindmap createMindmapFromRagRequest(RagMindmapRequest request, Long userId, String ragContext) {
        Mindmap mindmap = new Mindmap();
        mindmap.setTitle(request.getTopic());
        mindmap.setDescription(request.getDescription());
        mindmap.setUserId(userId);
        mindmap.setGrade(request.getGrade().toString());
        mindmap.setSubject(request.getSubject());
        mindmap.setIsPublic(false);
        mindmap.setIsAiGenerated(true);
        mindmap.setAiProvider(request.getAiProvider().name().toLowerCase());
        mindmap.setAiModel(request.getAiModel());
        mindmap.setCreatedAt(LocalDateTime.now());
        mindmap.setUpdatedAt(LocalDateTime.now());

        return mindmap;
    }

    @SuppressWarnings("unused")
    private List<MindmapNode> generateNodes(Mindmap mindmap, RagMindmapRequest request, String ragContext) {
        // Deprecated: Use parseNodesFromAiResponse instead
        List<MindmapNode> nodes = new ArrayList<>();
        MindmapNode rootNode = new MindmapNode();
        rootNode.setMindmapId(mindmap.getId());
        rootNode.setTitle(request.getTopic());
        rootNode.setContent("Root node for " + request.getTopic());
        rootNode.setNodeType(MindmapNode.NodeType.CONCEPT);
        rootNode.setLevel(0);
        rootNode.setPositionX(0.0);
        rootNode.setPositionY(0.0);
        rootNode.setCreatedAt(LocalDateTime.now());
        rootNode.setUpdatedAt(LocalDateTime.now());
        nodes.add(rootNode);
        return nodes;
    }

    /**
     * Call RAG service to generate mindmap structure with AI
     * The detailed prompt is handled by RAG service's mindmap-general-prompt.st template
     */
    private String callRagServiceForMindmapGeneration(RagMindmapRequest request, String ragContext) {
        log.info("Calling RAG service to generate mindmap for topic: {}", request.getTopic());

        try {
            // Simple query - RAG service will use mindmap-general-prompt.st for detailed instructions
            String query = String.format(
                "Tạo mindmap chi tiết về '%s' cho học sinh lớp %s. " +
                "Yêu cầu: TỐI THIỂU 15-20 nodes (4-5 branches, mỗi branch có 3-4 subBranches). " +
                "Mỗi node phải có content đầy đủ 200-500 từ với emoji, công thức toán học (², ³, √, Δ), " +
                "ví dụ cụ thể, và bài tập có đáp án. " +
                "KHÔNG viết nội dung chung chung hoặc '...'.",
                request.getTopic(),
                request.getGrade()
            );

            RagRequest ragRequest = RagRequest.builder()
                    .queryText(query)
                    .mode("MINDMAP")  // MINDMAP mode will use mindmap-general-prompt.st template
                    .llmProvider(request.getAiProvider().name())
                    .useDocuments(request.getUseDocuments())
                    .documentId(request.getDocumentId() != null ? request.getDocumentId().toString() : null)
                    .chapterId(request.getChapterId() != null ? request.getChapterId().toString() : null)
                    .lessonId(request.getLessonId() != null ? request.getLessonId().toString() : null)
                    .build();

            log.debug("Sending RAG request with useDocuments={}", request.getUseDocuments());
            RagResponse ragResponse = ragServiceClient.processRagQuery(ragRequest);

            if (ragResponse != null && ragResponse.getResponse() != null) {
                // Extract mindmap content from response
                Object responseObj = ragResponse.getResponse();
                log.debug("RAG response type: {}", responseObj.getClass().getName());

                // Try to extract mindmap content from response
                if (responseObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> responseMap = (java.util.Map<String, Object>) responseObj;
                    Object mindmapContent = responseMap.get("mindmapContent");
                    if (mindmapContent != null) {
                        String content = mindmapContent.toString();
                        log.info("Extracted mindmapContent from response map, length: {}", content.length());
                        return content;
                    }
                    // If no mindmapContent key, try to use the whole map
                    log.warn("No 'mindmapContent' key found, using whole response map");
                }

                // If response is already a string (JSON), return it
                if (responseObj instanceof String) {
                    String content = (String) responseObj;
                    log.info("Response is already string, length: {}", content.length());
                    return content;
                }

                // Try to convert object to JSON string
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonString = mapper.writeValueAsString(responseObj);
                log.info("Converted response to JSON string, length: {}", jsonString.length());
                return jsonString;
            }

            log.error("RAG service returned null or invalid response");
            throw new RuntimeException("RAG service returned null or invalid response");

        } catch (Exception e) {
            log.error("Failed to call RAG service for mindmap generation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate mindmap with AI: " + e.getMessage(), e);
        }
    }

    /**
     * Parse AI-generated JSON into MindmapNode entities
     */
    private List<MindmapNode> parseNodesFromAiResponse(Mindmap mindmap, String aiJsonResponse) {
        List<MindmapNode> nodes = new ArrayList<>();

        try {
            log.info("Parsing AI response JSON for mindmap: {}", mindmap.getId());
            log.debug("AI response (first 500 chars): {}", aiJsonResponse.substring(0, Math.min(500, aiJsonResponse.length())));

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(aiJsonResponse);

            // Check if response has expected structure
            if (!root.has("centralTopic") && !root.has("branches")) {
                log.warn("AI response missing expected structure (centralTopic/branches). Response keys: {}",
                    java.util.stream.StreamSupport.stream(
                        java.util.Spliterators.spliteratorUnknownSize(root.fieldNames(), java.util.Spliterator.ORDERED),
                        false)
                        .collect(java.util.stream.Collectors.joining(", ")));

                // Try to extract from nested structure if exists
                if (root.has("mindmapContent")) {
                    log.info("Found nested mindmapContent, trying to parse it");
                    root = mapper.readTree(root.get("mindmapContent").asText());
                }
            }

            // Create central topic node - WITHOUT parent (it's the root)
            MindmapNode centralNode = new MindmapNode();
            centralNode.setMindmapId(mindmap.getId());
            centralNode.setTitle(root.path("centralTopic").asText(mindmap.getTitle()));
            centralNode.setContent("Central topic of the mindmap");
            centralNode.setNodeType(MindmapNode.NodeType.CONCEPT);
            centralNode.setLevel(0);
            centralNode.setPositionX(0.0);
            centralNode.setPositionY(0.0);
            centralNode.setOrderIndex(0);  // Root node is always first
            centralNode.setParentNodeId(null);  // Central node has no parent
            centralNode.setCreatedAt(LocalDateTime.now());
            centralNode.setUpdatedAt(LocalDateTime.now());
            nodes.add(centralNode);

            // Parse branches (main nodes) WITHOUT setting parent yet
            com.fasterxml.jackson.databind.JsonNode branches = root.path("branches");
            List<MindmapNode> branchNodes = new ArrayList<>();  // Keep track of branch nodes for later
            if (branches.isArray() && branches.size() > 0) {
                log.info("Found {} main branches to parse", branches.size());
                int branchIndex = 0;
                for (com.fasterxml.jackson.databind.JsonNode branch : branches) {
                    MindmapNode branchNode = parseBranchNodeWithoutParent(mindmap, branch, 1, branchIndex);
                    nodes.add(branchNode);
                    branchNodes.add(branchNode);

                    // Parse sub-branches
                    com.fasterxml.jackson.databind.JsonNode subBranches = branch.path("subBranches");
                    if (subBranches.isArray() && subBranches.size() > 0) {
                        log.debug("Branch {} has {} sub-branches", branchIndex, subBranches.size());
                        int subIndex = 0;
                        for (com.fasterxml.jackson.databind.JsonNode subBranch : subBranches) {
                            MindmapNode subNode = parseSubBranchNodeWithoutParent(mindmap, subBranch, 2, branchIndex, subIndex);
                            nodes.add(subNode);
                            subIndex++;
                        }
                    }
                    branchIndex++;
                }
            } else {
                log.warn("No branches found in AI response or branches is not an array");
            }

            // NOTE: Parent relationships will be set later in setParentRelationships()
            // after nodes are saved and have generated IDs
            log.info("Successfully parsed {} nodes from AI response", nodes.size());

            if (nodes.size() <= 1) {
                log.error("Only {} node(s) created, falling back to default nodes", nodes.size());
                return createFallbackNodes(mindmap);
            }

            return nodes;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage(), e);
            log.error("AI response that failed to parse: {}", aiJsonResponse);
            // Return fallback with single root node
            return createFallbackNodes(mindmap);
        }
    }

    private MindmapNode parseBranchNode(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode branch,
                                        int level, int index, MindmapNode parentNode) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(branch.path("title").asText("Branch " + index));

        // Try to get description first, then fallback to content if not available
        String content = branch.path("description").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = branch.path("content").asText("");
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(branch.path("nodeType").asText("concept")));
        node.setLevel(level);
        node.setOrderIndex(index);  // Set order index for proper sorting

        // Position nodes in a circle around center
        double angle = (2 * Math.PI * index) / 8.0; // Assume max 8 branches
        double radius = 300.0;
        node.setPositionX(radius * Math.cos(angle));
        node.setPositionY(radius * Math.sin(angle));

        // Set parent relationship - will be persisted after parent node ID is generated
        node.setParentNodeId(parentNode.getId());

        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    // New version without parent parameter - parent will be set later
    private MindmapNode parseBranchNodeWithoutParent(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode branch,
                                                     int level, int index) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(branch.path("title").asText("Branch " + index));

        // Try to get description first, then fallback to content if not available
        String content = branch.path("description").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = branch.path("content").asText("");
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(branch.path("nodeType").asText("concept")));
        node.setLevel(level);
        node.setOrderIndex(index);  // Set order index for proper sorting

        // Position nodes in a circle around center
        double angle = (2 * Math.PI * index) / 8.0; // Assume max 8 branches
        double radius = 300.0;
        node.setPositionX(radius * Math.cos(angle));
        node.setPositionY(radius * Math.sin(angle));

        // Parent will be set later after IDs are generated
        node.setParentNodeId(null);

        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    private MindmapNode parseSubBranchNode(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode subBranch,
                                           int level, int branchIndex, int subIndex, MindmapNode parentNode) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(subBranch.path("title").asText("Sub-branch " + subIndex));

        // Try to get content first, then fallback to description if not available
        String content = subBranch.path("content").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = subBranch.path("description").asText("");
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(subBranch.path("nodeType").asText("concept")));
        node.setLevel(level);
        node.setOrderIndex(subIndex);  // Set order index for proper sorting

        // Position sub-nodes around their parent
        double parentAngle = (2 * Math.PI * branchIndex) / 8.0;
        double subAngle = parentAngle + (subIndex - 1) * 0.3; // Spread around parent
        double radius = 500.0;
        node.setPositionX(radius * Math.cos(subAngle));
        node.setPositionY(radius * Math.sin(subAngle));

        // Set parent relationship to the branch node
        node.setParentNodeId(parentNode.getId());

        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    // New version without parent parameter - parent will be set later
    private MindmapNode parseSubBranchNodeWithoutParent(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode subBranch,
                                                        int level, int branchIndex, int subIndex) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(subBranch.path("title").asText("Sub-branch " + subIndex));

        // Try to get content first, then fallback to description if not available
        String content = subBranch.path("content").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = subBranch.path("description").asText("");
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(subBranch.path("nodeType").asText("concept")));
        node.setLevel(level);
        node.setOrderIndex(subIndex);  // Set order index for proper sorting

        // Position sub-nodes around their parent
        double parentAngle = (2 * Math.PI * branchIndex) / 8.0;
        double subAngle = parentAngle + (subIndex - 1) * 0.3; // Spread around parent
        double radius = 500.0;
        node.setPositionX(radius * Math.cos(subAngle));
        node.setPositionY(radius * Math.sin(subAngle));

        // Parent will be set later after IDs are generated
        node.setParentNodeId(null);

        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    private MindmapNode.NodeType parseNodeType(String typeStr) {
        try {
            return MindmapNode.NodeType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return MindmapNode.NodeType.CONCEPT;
        }
    }

    private List<MindmapNode> createFallbackNodes(Mindmap mindmap) {
        List<MindmapNode> nodes = new ArrayList<>();
        
        // Create central node
        MindmapNode rootNode = new MindmapNode();
        rootNode.setMindmapId(mindmap.getId());
        rootNode.setTitle(mindmap.getTitle());
        rootNode.setContent("Chủ đề trung tâm của mindmap");
        rootNode.setNodeType(MindmapNode.NodeType.CONCEPT);
        rootNode.setLevel(0);
        rootNode.setPositionX(0.0);
        rootNode.setPositionY(0.0);
        rootNode.setOrderIndex(0);
        rootNode.setParentNodeId(null);
        rootNode.setCreatedAt(LocalDateTime.now());
        rootNode.setUpdatedAt(LocalDateTime.now());
        nodes.add(rootNode);
        
        // Create 4 default branch nodes for fallback
        String[] branchTitles = {"Định nghĩa", "Công thức", "Ví dụ", "Bài tập"};
        String[] branchContents = {
            "Các khái niệm cơ bản và định nghĩa quan trọng",
            "Các công thức toán học liên quan",
            "Ví dụ minh họa cụ thể",
            "Bài tập thực hành"
        };
        
        for (int i = 0; i < 4; i++) {
            MindmapNode branchNode = new MindmapNode();
            branchNode.setMindmapId(mindmap.getId());
            branchNode.setTitle(branchTitles[i]);
            branchNode.setContent(branchContents[i]);
            branchNode.setNodeType(MindmapNode.NodeType.CONCEPT);
            branchNode.setLevel(1);
            branchNode.setOrderIndex(i);
            branchNode.setParentNodeId(null);  // Will be set after save
            
            // Position in circle
            double angle = (2 * Math.PI * i) / 4.0;
            double radius = 300.0;
            branchNode.setPositionX(radius * Math.cos(angle));
            branchNode.setPositionY(radius * Math.sin(angle));
            
            branchNode.setCreatedAt(LocalDateTime.now());
            branchNode.setUpdatedAt(LocalDateTime.now());
            nodes.add(branchNode);
        }
        
        log.info("Created fallback mindmap with {} nodes", nodes.size());
        return nodes;
    }

    /**
     * Set parent relationships based on node levels
     * Level 0 (central) has no parent
     * Level 1 (branches) parent is the Level 0 node
     * Level 2 (sub-branches) parent is the closest Level 1 node by position
     */
    private void setParentRelationships(List<MindmapNode> nodes) {
        // Find the central node (level 0)
        MindmapNode centralNode = nodes.stream()
            .filter(n -> n.getLevel() == 0)
            .findFirst()
            .orElse(null);

        if (centralNode == null) {
            log.warn("No central node found (level 0)");
            return;
        }

        // Get all level 1 nodes (branches)
        List<MindmapNode> level1Nodes = nodes.stream()
            .filter(n -> n.getLevel() == 1)
            .toList();

        // Set all level 1 nodes' parent to central node
        for (MindmapNode node : level1Nodes) {
            node.setParentNodeId(centralNode.getId());
            log.debug("Set level 1 node {} parent to central node {}", node.getId(), centralNode.getId());
        }

        // Get all level 2 nodes (sub-branches)
        List<MindmapNode> level2Nodes = nodes.stream()
            .filter(n -> n.getLevel() == 2)
            .toList();

        // For each level 2 node, find closest level 1 node as parent
        for (MindmapNode level2Node : level2Nodes) {
            MindmapNode closestParent = findClosestParent(level2Node, level1Nodes);
            if (closestParent != null) {
                level2Node.setParentNodeId(closestParent.getId());
                log.debug("Set level 2 node {} parent to level 1 node {}", level2Node.getId(), closestParent.getId());
            }
        }
    }

    /**
     * Find the closest level 1 node by position to a given level 2 node
     */
    private MindmapNode findClosestParent(MindmapNode level2Node, List<MindmapNode> level1Nodes) {
        if (level1Nodes.isEmpty()) {
            return null;
        }

        MindmapNode closest = level1Nodes.get(0);
        double minDistance = calculateDistance(level2Node, closest);

        for (MindmapNode candidate : level1Nodes) {
            double distance = calculateDistance(level2Node, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    /**
     * Calculate Euclidean distance between two nodes based on their positions
     */
    private double calculateDistance(MindmapNode node1, MindmapNode node2) {
        double dx = (node1.getPositionX() != null ? node1.getPositionX() : 0) -
                    (node2.getPositionX() != null ? node2.getPositionX() : 0);
        double dy = (node1.getPositionY() != null ? node1.getPositionY() : 0) -
                    (node2.getPositionY() != null ? node2.getPositionY() : 0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Generate edges based on node hierarchy using parentNodeId
     */
    private List<MindmapEdge> generateEdgesFromNodes(Mindmap mindmap, List<MindmapNode> nodes) {
        List<MindmapEdge> edges = new ArrayList<>();

        if (nodes.isEmpty()) {
            return edges;
        }

        // Create edges based on parent-child relationships defined in parentNodeId
        for (MindmapNode node : nodes) {
            if (node.getParentNodeId() != null) {
                // Find parent node by ID
                MindmapNode parentNode = nodes.stream()
                    .filter(n -> n.getId() != null && n.getId().equals(node.getParentNodeId()))
                    .findFirst()
                    .orElse(null);

                if (parentNode != null) {
                    MindmapEdge edge = new MindmapEdge();
                    edge.setMindmapId(mindmap.getId());
                    edge.setFromNodeId(parentNode.getId());
                    edge.setToNodeId(node.getId());
                    edge.setLabel("");
                    edge.setCreatedAt(LocalDateTime.now());
                    edges.add(edge);
                    log.debug("Created edge from node {} to node {}", parentNode.getId(), node.getId());
                }
            }
        }

        log.info("Generated {} edges from {} nodes", edges.size(), nodes.size());
        return edges;
    }

    private RagMindmapResponse createErrorResponse(String errorMessage) {
        return RagMindmapResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
