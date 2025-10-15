package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.mindmapservice.dto.ai.*;
import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapEdge;
import com.mss301.mindmapservice.entity.MindmapNode;
import com.mss301.mindmapservice.service.AiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.mistral.api-key}")
    private String mistralApiKey;

    @Value("${ai.mistral.base-url}")
    private String mistralBaseUrl;

    @Value("${ai.mistral.model}")
    private String mistralModel;

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${ai.gemini.models.primary}")
    private String geminiPrimaryModel;

    @Value("${ai.gemini.models.fallback-1}")
    private String geminiFallback1;

    @Value("${ai.gemini.models.fallback-2}")
    private String geminiFallback2;

    @Value("${ai.service.max-retries:3}")
    private int maxRetries;

    @Value("${ai.service.retry-delay:1000}")
    private long retryDelay;

    @Override
    public AiGenerateMindmapResponse generateMindmap(AiGenerateMindmapRequest request, Long userId) {
        log.info("Generating mindmap for user: {} with provider: {}", userId, request.getAiProvider());

        try {
            String aiProvider = request.getAiProvider().name().toLowerCase();
            String prompt = buildMindmapPrompt(request);

            String response = callAiService(aiProvider, prompt, request.getAiModel());

            // Parse AI response and create mindmap structure
            Mindmap mindmap = createMindmapFromAiResponse(request, userId, response);
            List<MindmapNode> nodes = generateNodes(mindmap, request);
            List<MindmapEdge> edges = generateEdges(mindmap, nodes, request);

            return AiGenerateMindmapResponse.builder()
                    .mindmapId(mindmap.getId())
                    .title(mindmap.getTitle())
                    .description(mindmap.getDescription())
                    .aiProvider(aiProvider)
                    .aiModel(request.getAiModel())
                    .status("SUCCESS")
                    .nodesGenerated(nodes.size())
                    .edgesGenerated(edges.size())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate mindmap: {}", e.getMessage(), e);
            return AiGenerateMindmapResponse.builder()
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public List<MindmapNode> generateNodes(Mindmap mindmap, AiGenerateMindmapRequest request) {
        log.info("Generating nodes for mindmap: {}", mindmap.getId());

        try {
            String prompt = buildNodesPrompt(request);
            String response = callAiService(request.getAiProvider().name().toLowerCase(), prompt, request.getAiModel());

            return parseNodesFromResponse(response, mindmap.getId());

        } catch (Exception e) {
            log.error("Failed to generate nodes: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<MindmapEdge> generateEdges(Mindmap mindmap, List<MindmapNode> nodes, AiGenerateMindmapRequest request) {
        log.info("Generating edges for mindmap: {}", mindmap.getId());

        try {
            String prompt = buildEdgesPrompt(nodes, request);
            String response = callAiService(request.getAiProvider().name().toLowerCase(), prompt, request.getAiModel());

            return parseEdgesFromResponse(response, mindmap.getId(), nodes);

        } catch (Exception e) {
            log.error("Failed to generate edges: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getAvailableModels(String provider) {
        List<String> models = new ArrayList<>();

        if ("mistral".equals(provider)) {
            models.add("mistral-large-latest");
            models.add("mistral-medium-latest");
            models.add("mistral-small-latest");
        } else if ("gemini".equals(provider)) {
            models.add("gemini-2.5-flash-preview-09-2025");
            models.add("gemini-1.5-pro");
            models.add("gemini-2.0-flash");
        }

        return models;
    }

    @Override
    public boolean isServiceHealthy(String provider) {
        try {
            String testPrompt = "Test connection";
            callAiService(provider, testPrompt, null);
            return true;
        } catch (Exception e) {
            log.warn("AI service health check failed for provider: {}", provider, e);
            return false;
        }
    }

    private String callAiService(String provider, String prompt, String model) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if ("mistral".equals(provider)) {
                    return callMistralApi(prompt, model);
                } else if ("gemini".equals(provider)) {
                    return callGeminiApi(prompt, model);
                } else {
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
                }
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().is4xxClientError() && attempt < maxRetries) {
                    log.warn("AI service call failed, retrying... Attempt: {}/{}", attempt, maxRetries);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("All retry attempts failed");
    }

    private String callMistralApi(String prompt, String model) {
        String actualModel = model != null ? model : mistralModel;

        MistralRequest request = MistralRequest.builder()
                .model(actualModel)
                .messages(List.of(MistralRequest.MistralMessage.builder()
                        .role("user")
                        .content(prompt)
                        .build()))
                .maxTokens(4000)
                .temperature(0.7)
                .topP(0.9)
                .stream(false)
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(mistralBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + mistralApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        MistralResponse response = webClient
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MistralResponse.class)
                .block();

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        throw new RuntimeException("No response from Mistral API");
    }

    private String callGeminiApi(String prompt, String model) {
        String actualModel = model != null ? model : geminiPrimaryModel;

        // Try primary model first, then fallback models
        List<String> modelsToTry = Arrays.asList(actualModel, geminiFallback1, geminiFallback2);

        for (String modelToTry : modelsToTry) {
            try {
                return callGeminiWithModel(prompt, modelToTry);
            } catch (Exception e) {
                log.warn("Failed to call Gemini with model: {}, trying next model", modelToTry);
                if (modelToTry.equals(geminiFallback2)) {
                    throw e; // Last model failed
                }
            }
        }

        throw new RuntimeException("All Gemini models failed");
    }

    private String callGeminiWithModel(String prompt, String model) {
        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(GeminiRequest.GeminiContent.builder()
                        .parts(List.of(
                                GeminiRequest.GeminiPart.builder().text(prompt).build()))
                        .role("user")
                        .build()))
                .generationConfig(GeminiRequest.GeminiGenerationConfig.builder()
                        .maxOutputTokens(4000)
                        .temperature(0.7)
                        .topP(0.9)
                        .topK(40.0)
                        .build())
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(geminiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        GeminiResponse response = webClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, geminiApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            return response.getCandidates()
                    .get(0)
                    .getContent()
                    .getParts()
                    .get(0)
                    .getText();
        }

        throw new RuntimeException("No response from Gemini API");
    }

    private String buildMindmapPrompt(AiGenerateMindmapRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a comprehensive mindmap for the following topic:\n\n");
        prompt.append("Topic: ").append(request.getTopic()).append("\n");
        prompt.append("Grade: ").append(request.getGrade()).append("\n");
        prompt.append("Subject: ").append(request.getSubject()).append("\n");

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            prompt.append("Description: ").append(request.getDescription()).append("\n");
        }

        if (request.getAdditionalContext() != null
                && !request.getAdditionalContext().isEmpty()) {
            prompt.append("Additional Context: ")
                    .append(request.getAdditionalContext())
                    .append("\n");
        }

        prompt.append("\nRequirements:\n");
        prompt.append("- Maximum ").append(request.getMaxNodes()).append(" nodes\n");
        prompt.append("- Maximum depth of ").append(request.getMaxDepth()).append(" levels\n");
        prompt.append("- Include examples: ")
                .append(request.getIncludeExamples() ? "Yes" : "No")
                .append("\n");
        prompt.append("- Include exercises: ")
                .append(request.getIncludeExercises() ? "Yes" : "No")
                .append("\n");
        prompt.append("- Include formulas: ")
                .append(request.getIncludeFormulas() ? "Yes" : "No")
                .append("\n");

        prompt.append("\nPlease provide a structured mindmap in JSON format with nodes and their relationships.");

        return prompt.toString();
    }

    private String buildNodesPrompt(AiGenerateMindmapRequest request) {
        return "Generate detailed nodes for the mindmap topic: " + request.getTopic() + " for grade "
                + request.getGrade() + " in " + request.getSubject()
                + ". Return as JSON array of nodes with title, content, nodeType, and level.";
    }

    private String buildEdgesPrompt(List<MindmapNode> nodes, AiGenerateMindmapRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate relationships between these nodes:\n");

        for (MindmapNode node : nodes) {
            prompt.append("- ")
                    .append(node.getTitle())
                    .append(" (ID: ")
                    .append(node.getId())
                    .append(")\n");
        }

        prompt.append("\nReturn as JSON array of edges with fromNodeId, toNodeId, and relationshipType.");

        return prompt.toString();
    }

    private Mindmap createMindmapFromAiResponse(AiGenerateMindmapRequest request, Long userId, String aiResponse) {
        Mindmap mindmap = new Mindmap();
        mindmap.setTitle(request.getTopic());
        mindmap.setDescription(request.getDescription());
        mindmap.setUserId(userId);
        mindmap.setGrade(request.getGrade());
        mindmap.setSubject(request.getSubject());
        mindmap.setIsPublic(false);
        mindmap.setIsAiGenerated(true);
        mindmap.setAiProvider(request.getAiProvider().name().toLowerCase());
        mindmap.setAiModel(request.getAiModel());
        mindmap.setCreatedAt(LocalDateTime.now());
        mindmap.setUpdatedAt(LocalDateTime.now());

        return mindmap;
    }

    private List<MindmapNode> parseNodesFromResponse(String response, Long mindmapId) {
        List<MindmapNode> nodes = new ArrayList<>();

        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    MindmapNode mindmapNode = new MindmapNode();
                    mindmapNode.setMindmapId(mindmapId);
                    mindmapNode.setTitle(node.get("title").asText());
                    mindmapNode.setContent(node.get("content").asText());
                    mindmapNode.setNodeType(
                            MindmapNode.NodeType.valueOf(node.get("nodeType").asText()));
                    mindmapNode.setLevel(node.get("level").asInt());
                    mindmapNode.setPositionX(
                            node.has("positionX") ? node.get("positionX").asDouble() : 0.0);
                    mindmapNode.setPositionY(
                            node.has("positionY") ? node.get("positionY").asDouble() : 0.0);
                    mindmapNode.setCreatedAt(LocalDateTime.now());
                    mindmapNode.setUpdatedAt(LocalDateTime.now());

                    nodes.add(mindmapNode);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse nodes from AI response: {}", e.getMessage(), e);
        }

        return nodes;
    }

    private List<MindmapEdge> parseEdgesFromResponse(String response, Long mindmapId, List<MindmapNode> nodes) {
        List<MindmapEdge> edges = new ArrayList<>();

        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.isArray()) {
                for (JsonNode edge : jsonNode) {
                    MindmapEdge mindmapEdge = new MindmapEdge();
                    mindmapEdge.setMindmapId(mindmapId);
                    mindmapEdge.setFromNodeId(edge.get("fromNodeId").asLong());
                    mindmapEdge.setToNodeId(edge.get("toNodeId").asLong());
                    mindmapEdge.setRelationshipType(edge.get("relationshipType").asText());
                    mindmapEdge.setIsDirected(true);
                    mindmapEdge.setCreatedAt(LocalDateTime.now());
                    mindmapEdge.setUpdatedAt(LocalDateTime.now());

                    edges.add(mindmapEdge);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse edges from AI response: {}", e.getMessage(), e);
        }

        return edges;
    }
}
