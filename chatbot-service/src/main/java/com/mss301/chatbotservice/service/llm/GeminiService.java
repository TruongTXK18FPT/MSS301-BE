package com.mss301.chatbotservice.service.llm;

import com.mss301.chatbotservice.exception.ChatbotServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public GeminiService(
            RestTemplate restTemplate,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        
        // Validate API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("GEMINI_API_KEY is not set! Please set it in environment variable or .env file");
            throw new IllegalStateException("GEMINI_API_KEY is required but not configured. Please set GEMINI_API_KEY environment variable or add it to .env file");
        }
        
        log.info("GeminiService initialized with model: {}, API key length: {}", model, apiKey.length());
    }

    public String generateResponse(String prompt) {
        log.info("Generating response using Gemini 2.5 Flash");
        
        // Double-check API key before making request
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ChatbotServiceException("GEMINI_API_KEY is not configured");
        }

        try {
            String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
            );
            
            log.debug("Calling Gemini API with URL (key hidden): https://generativelanguage.googleapis.com/v1beta/models/{}/generateContent?key=***", model);

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

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {});

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ChatbotServiceException("Gemini API response body is null");
            }

            String responseText = extractTextFromResponse(responseBody);

            log.info("Gemini AI response generated successfully. Length: {} characters", responseText.length());
            return responseText;

        } catch (Exception e) {
            log.error("Error calling Gemini AI", e);
            throw new ChatbotServiceException("Failed to generate response from Gemini AI: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<?, ?> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            throw new ChatbotServiceException("Invalid response structure from Gemini API");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseBody, e);
            throw new ChatbotServiceException("Failed to parse Gemini API response", e);
        }
    }
}

