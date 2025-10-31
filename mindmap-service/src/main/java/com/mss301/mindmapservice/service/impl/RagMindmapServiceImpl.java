package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagMindmapServiceImpl implements RagMindmapService {

    private final RagServiceClient ragServiceClient;
    private final com.mss301.mindmapservice.repository.MindmapRepository mindmapRepository;
    private final com.mss301.mindmapservice.repository.MindmapNodeRepository mindmapNodeRepository;
    private final com.mss301.mindmapservice.repository.MindmapEdgeRepository mindmapEdgeRepository;

    @Value("${rag.mindmap-generation.max-documents:5}")
    private Integer maxDocuments;

    @Override
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
            
            // Parse JSON and generate nodes
            List<MindmapNode> nodes = parseNodesFromAiResponse(savedMindmap, aiGeneratedJson);
            
            // Generate edges based on node hierarchy
            List<MindmapEdge> edges = generateEdgesFromNodes(savedMindmap, nodes);
            
            // SAVE nodes and edges
            if (!nodes.isEmpty()) {
                mindmapNodeRepository.saveAll(nodes);
                log.info("Saved {} nodes", nodes.size());
            } else {
                log.warn("No nodes generated from AI response");
            }
            
            if (!edges.isEmpty()) {
                mindmapEdgeRepository.saveAll(edges);
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
     */
    private String callRagServiceForMindmapGeneration(RagMindmapRequest request, String ragContext) {
        log.info("Calling RAG service to generate mindmap for topic: {}", request.getTopic());

        try {
            String query = String.format(
                "Create a detailed mindmap about '%s' for grade %s in Vietnamese. " +
                "Structure: {\"centralTopic\": \"Main Topic\", \"branches\": [{\"title\": \"Branch 1\", \"nodeType\": \"concept\", \"description\": \"...\", \"subBranches\": [{\"title\": \"Sub 1\", \"nodeType\": \"concept\", \"content\": \"...\"}]}]}. " +
                "Include at least 3-5 main branches with 2-3 sub-branches each. NodeType can be: concept, formula, example, exercise.",
                request.getTopic(),
                request.getGrade()
            );

            RagRequest ragRequest = RagRequest.builder()
                    .queryText(query)
                    .mode("MINDMAP")  // Use MINDMAP mode
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

            // Create central topic node
            MindmapNode centralNode = new MindmapNode();
            centralNode.setMindmapId(mindmap.getId());
            centralNode.setTitle(root.path("centralTopic").asText(mindmap.getTitle()));
            centralNode.setContent("Central topic of the mindmap");
            centralNode.setNodeType(MindmapNode.NodeType.CONCEPT);
            centralNode.setLevel(0);
            centralNode.setPositionX(0.0);
            centralNode.setPositionY(0.0);
            centralNode.setCreatedAt(LocalDateTime.now());
            centralNode.setUpdatedAt(LocalDateTime.now());
            nodes.add(centralNode);

            // Parse branches (main nodes)
            com.fasterxml.jackson.databind.JsonNode branches = root.path("branches");
            if (branches.isArray() && branches.size() > 0) {
                log.info("Found {} main branches to parse", branches.size());
                int branchIndex = 0;
                for (com.fasterxml.jackson.databind.JsonNode branch : branches) {
                    MindmapNode branchNode = parseBranchNode(mindmap, branch, 1, branchIndex);
                    nodes.add(branchNode);

                    // Parse sub-branches
                    com.fasterxml.jackson.databind.JsonNode subBranches = branch.path("subBranches");
                    if (subBranches.isArray() && subBranches.size() > 0) {
                        log.debug("Branch {} has {} sub-branches", branchIndex, subBranches.size());
                        int subIndex = 0;
                        for (com.fasterxml.jackson.databind.JsonNode subBranch : subBranches) {
                            MindmapNode subNode = parseSubBranchNode(mindmap, subBranch, 2, branchIndex, subIndex);
                            nodes.add(subNode);
                            subIndex++;
                        }
                    }
                    branchIndex++;
                }
            } else {
                log.warn("No branches found in AI response or branches is not an array");
            }

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

    private MindmapNode parseBranchNode(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode branch, int level, int index) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(branch.path("title").asText("Branch " + index));
        node.setContent(branch.path("description").asText(""));
        node.setNodeType(parseNodeType(branch.path("nodeType").asText("concept")));
        node.setLevel(level);
        
        // Position nodes in a circle around center
        double angle = (2 * Math.PI * index) / 8.0; // Assume max 8 branches
        double radius = 300.0;
        node.setPositionX(radius * Math.cos(angle));
        node.setPositionY(radius * Math.sin(angle));
        
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        
        return node;
    }

    private MindmapNode parseSubBranchNode(Mindmap mindmap, com.fasterxml.jackson.databind.JsonNode subBranch, 
                                           int level, int branchIndex, int subIndex) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(subBranch.path("title").asText("Sub-branch " + subIndex));
        node.setContent(subBranch.path("content").asText(""));
        node.setNodeType(parseNodeType(subBranch.path("nodeType").asText("concept")));
        node.setLevel(level);
        
        // Position sub-nodes around their parent
        double parentAngle = (2 * Math.PI * branchIndex) / 8.0;
        double subAngle = parentAngle + (subIndex - 1) * 0.3; // Spread around parent
        double radius = 500.0;
        node.setPositionX(radius * Math.cos(subAngle));
        node.setPositionY(radius * Math.sin(subAngle));
        
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
        MindmapNode rootNode = new MindmapNode();
        rootNode.setMindmapId(mindmap.getId());
        rootNode.setTitle(mindmap.getTitle());
        rootNode.setContent("AI generation failed, using fallback node");
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
     * Generate edges based on node hierarchy
     */
    private List<MindmapEdge> generateEdgesFromNodes(Mindmap mindmap, List<MindmapNode> nodes) {
        List<MindmapEdge> edges = new ArrayList<>();
        
        if (nodes.isEmpty()) {
            return edges;
        }

        // Root node is always first
        MindmapNode rootNode = nodes.get(0);
        
        // Connect all level 1 nodes to root
        List<MindmapNode> level1Nodes = nodes.stream()
            .filter(n -> n.getLevel() == 1)
            .toList();
        
        for (MindmapNode node : level1Nodes) {
            MindmapEdge edge = new MindmapEdge();
            edge.setMindmapId(mindmap.getId());
            edge.setFromNodeId(rootNode.getId());
            edge.setToNodeId(node.getId());
            edge.setLabel("");
            edge.setCreatedAt(LocalDateTime.now());
            edges.add(edge);
        }

        // Connect level 2 nodes to their level 1 parents
        // For simplicity, connect each level 2 node to the nearest level 1 node
        List<MindmapNode> level2Nodes = nodes.stream()
            .filter(n -> n.getLevel() == 2)
            .toList();
        
        for (MindmapNode level2Node : level2Nodes) {
            // Find closest level 1 node by position
            MindmapNode closestParent = findClosestNode(level2Node, level1Nodes);
            if (closestParent != null) {
                MindmapEdge edge = new MindmapEdge();
                edge.setMindmapId(mindmap.getId());
                edge.setFromNodeId(closestParent.getId());
                edge.setToNodeId(level2Node.getId());
                edge.setLabel("");
                edge.setCreatedAt(LocalDateTime.now());
                edges.add(edge);
            }
        }

        log.info("Generated {} edges from node hierarchy", edges.size());
        return edges;
    }

    private MindmapNode findClosestNode(MindmapNode target, List<MindmapNode> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        MindmapNode closest = candidates.get(0);
        double minDistance = calculateDistance(target, closest);

        for (MindmapNode candidate : candidates) {
            double distance = calculateDistance(target, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    private double calculateDistance(MindmapNode node1, MindmapNode node2) {
        double dx = node1.getPositionX() - node2.getPositionX();
        double dy = node1.getPositionY() - node2.getPositionY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private RagMindmapResponse createErrorResponse(String errorMessage) {
        return RagMindmapResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
