package com.mss301.ragservice.service.llm;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.exception.UnsupportedLLMException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMServiceFactory {

    private final Map<LLMProvider, LLMService> llmServices;

    public LLMService getLLMService(LLMProvider provider) {
        log.debug("Requesting LLM service for provider: {}", provider);

        LLMService service = llmServices.get(provider);

        if (service == null) {
            log.error("LLM provider not configured: {}", provider);
            throw new UnsupportedLLMException("LLM provider not configured: " + provider);
        }

        if (!service.isAvailable()) {
            log.warn("LLM provider not available: {}, attempting fallback", provider);
            // Fallback: Try Gemini if Mistral fails
            if (provider == LLMProvider.MISTRAL) {
                log.info("Falling back to Gemini due to Mistral unavailability");
                LLMService fallbackService = llmServices.get(LLMProvider.GEMINI);
                if (fallbackService != null && fallbackService.isAvailable()) {
                    return fallbackService;
                }
            }
            throw new UnsupportedLLMException("LLM provider not available: " + provider);
        }

        log.info("Using LLM provider: {}", provider);
        return service;
    }
    
    /**
     * Get LLM service with automatic fallback
     */
    public LLMService getLLMServiceWithFallback(LLMProvider provider) {
        try {
            return getLLMService(provider);
        } catch (Exception e) {
            log.warn("Failed to get {} service, trying fallback: {}", provider, e.getMessage());
            
            // Try fallback providers
            if (provider == LLMProvider.MISTRAL) {
                try {
                    log.info("Attempting fallback to Gemini");
                    return getLLMService(LLMProvider.GEMINI);
                } catch (Exception fallbackEx) {
                    log.error("Fallback to Gemini also failed", fallbackEx);
                }
            }
            
            throw e;
        }
    }
}
