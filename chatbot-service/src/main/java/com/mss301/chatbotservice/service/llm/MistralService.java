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
public class MistralService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public MistralService(
            RestTemplate restTemplate,
            @Value("${mistral.api-key:}") String apiKey,
            @Value("${mistral.model:mistral-large-latest}") String model,
            @Value("${mistral.api-url:https://api.mistral.ai/v1/chat/completions}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        
        // Validate API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("CHATBOT_MISTRAL_API_KEY is not set! Please set it in environment variable or .env file");
            throw new IllegalStateException("CHATBOT_MISTRAL_API_KEY is required but not configured. Please set CHATBOT_MISTRAL_API_KEY environment variable or add it to .env file");
        }
        
        log.info("MistralService initialized with model: {}, API key length: {}", model, apiKey.length());
    }

    public String generateResponse(String prompt) {
        log.info("Generating response using Mistral AI");

        try {
            // Build request body for Mistral API
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                ),
                "temperature", 0.7,
                "max_tokens", 2048
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {});

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ChatbotServiceException("Mistral API response body is null");
            }

            String responseText = extractTextFromResponse(responseBody);

            log.info("Mistral AI response generated successfully. Length: {} characters", responseText.length());
            return responseText;

        } catch (Exception e) {
            log.error("Error calling Mistral AI", e);
            throw new ChatbotServiceException("Failed to generate response from Mistral AI: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<?, ?> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            throw new ChatbotServiceException("Invalid response structure from Mistral API");
        } catch (Exception e) {
            log.error("Failed to parse Mistral response: {}", responseBody, e);
            throw new ChatbotServiceException("Failed to parse Mistral API response", e);
        }
    }
}

