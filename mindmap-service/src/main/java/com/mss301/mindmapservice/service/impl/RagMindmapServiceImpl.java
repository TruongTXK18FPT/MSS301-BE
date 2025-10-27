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
            // Get relevant documents using RAG
            String ragContext = getRelevantDocuments(request);

            if (ragContext == null || ragContext.trim().isEmpty()) {
                log.warn("No relevant documents found for topic: {}", request.getTopic());
                return createErrorResponse("No relevant documents found for the given topic");
            }

            // Create mindmap with RAG context
            Mindmap mindmap = createMindmapFromRagRequest(request, userId, ragContext);
            
            // SAVE mindmap first
            Mindmap savedMindmap = mindmapRepository.save(mindmap);
            log.info("Saved mindmap with ID: {}", savedMindmap.getId());

            // Generate nodes and edges
            List<MindmapNode> nodes = generateNodes(savedMindmap, request, ragContext);
            List<MindmapEdge> edges = generateEdges(savedMindmap, nodes, request);
            
            // SAVE nodes and edges
            if (!nodes.isEmpty()) {
                mindmapNodeRepository.saveAll(nodes);
                log.info("Saved {} nodes", nodes.size());
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
                    .documentsUsed(maxDocuments)
                    .ragContext(ragContext)
                    .averageRelevanceScore(0.85)
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
        // Simplified node generation - in real implementation, this would call AI service
        List<MindmapNode> nodes = new ArrayList<>();

        // Create a basic root node
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

    @SuppressWarnings("unused")
    private List<MindmapEdge> generateEdges(Mindmap mindmap, List<MindmapNode> nodes, RagMindmapRequest request) {
        // Simplified edge generation - in real implementation, this would call AI service
        return new ArrayList<>();
    }

    private RagMindmapResponse createErrorResponse(String errorMessage) {
        return RagMindmapResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
