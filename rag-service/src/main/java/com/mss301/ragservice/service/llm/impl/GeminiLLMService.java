package com.mss301.ragservice.service.llm.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mss301.ragservice.enums.ResponseMode;
import com.mss301.ragservice.exception.RagServiceException;
import com.mss301.ragservice.prompt.PromptTemplateFactory;
import com.mss301.ragservice.service.llm.LLMService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiLLMService implements LLMService {

    private final RestTemplate restTemplate;
    private final PromptTemplateFactory promptTemplateFactory;
    
    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Value("${gemini.model}")
    private String model;

    public GeminiLLMService(
            RestTemplate restTemplate,
            PromptTemplateFactory promptTemplateFactory) {
        this.restTemplate = restTemplate;
        this.promptTemplateFactory = promptTemplateFactory;
        log.info("GeminiLLMService initialized");
    }
    
    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        // Validate API key after Spring injection
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Gemini API key is not set! Please set RAG_GEMINI_API_KEY environment variable or gemini.api-key property.");
            throw new IllegalStateException("Gemini API key is not configured");
        }
        
        if (model == null || model.isBlank()) {
            log.error("Gemini model is not set! Please set gemini.model property.");
            throw new IllegalStateException("Gemini model is not configured");
        }
        
        log.info("GeminiLLMService configuration validated - API key (length: {}), model: {}", apiKey.length(), model);
    }

    @Override
    public String generateResponse(String query, String context, ResponseMode mode) {
        log.info("Generating {} response using Gemini AI", mode);

        try {
            String prompt = buildPrompt(query, context, mode);
            log.debug("Prompt length: {} characters", prompt.length());

            String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
            );

            // Build request body for Gemini API
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", prompt)
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 2048
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            // Extract response text from Gemini API response
            @SuppressWarnings("unchecked")
            String responseText = extractTextFromResponse((Map<String, Object>) response.getBody());
            
            log.info("Gemini AI response generated successfully. Length: {} characters", responseText.length());
            return responseText;

        } catch (Exception e) {
            log.error("Error calling Gemini AI", e);
            throw new RagServiceException("Failed to generate response from Gemini AI: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        // Check if API key is set
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not set");
            return false;
        }
        
        // Check if model is set
        if (model == null || model.isBlank()) {
            log.warn("Gemini model is not set");
            return false;
        }
        
        // Optionally do a lightweight availability check
        // For now, just check if configuration is valid
        log.debug("Gemini AI configuration is valid (API key and model are set)");
        return true;
    }

    private String buildPrompt(String query, String context, ResponseMode mode) {
        return switch (mode) {
            case CHAT, VOICECHAT -> promptTemplateFactory.createChatPrompt(context, query);
            case MINDMAP -> promptTemplateFactory.createMindmapPrompt(context, query);
            case EXERCISE -> promptTemplateFactory.createExercisePrompt(context, query);
        };
    }
    
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            throw new RuntimeException("Invalid response structure from Gemini API");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseBody, e);
            throw new RagServiceException("Failed to parse Gemini API response", e);
        }
    }
}
