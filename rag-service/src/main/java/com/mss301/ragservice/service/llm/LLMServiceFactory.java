package com.mss301.ragservice.service.llm;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.exception.UnsupportedLLMException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

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
            throw new UnsupportedLLMException(
                    "LLM provider not configured: " + provider
            );
        }

        if (!service.isAvailable()) {
            log.error("LLM provider not available: {}", provider);
            throw new UnsupportedLLMException(
                    "LLM provider not available: " + provider
            );
        }

        log.info("Using LLM provider: {}", provider);
        return service;
    }
}
