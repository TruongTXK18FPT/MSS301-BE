package com.mss301.ragservice.service.llm.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.mss301.ragservice.enums.ResponseMode;
import com.mss301.ragservice.exception.RagServiceException;
import com.mss301.ragservice.prompt.PromptTemplateFactory;
import com.mss301.ragservice.service.llm.LLMService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistralLLMService implements LLMService {

    private final ChatClient mistralChatClient;
    private final PromptTemplateFactory promptTemplateFactory; // ⭐ Inject factory

    @Override
    public String generateResponse(String query, String context, ResponseMode mode) {
        log.info("Generating {} response using Mistral AI", mode);

        try {
            String prompt = buildPrompt(query, context, mode);

            log.debug("Prompt length: {} characters", prompt.length());

            String response = mistralChatClient.prompt().user(prompt).call().content();

            log.info("Mistral AI response generated successfully. Length: {} characters", response.length());

            return response;

        } catch (Exception e) {
            log.error("Error calling Mistral AI", e);
            throw new RagServiceException("Failed to generate response from Mistral AI: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            log.debug("Checking Mistral AI availability");

            mistralChatClient.prompt().user("ping").call().content();

            log.debug("Mistral AI is available");
            return true;

        } catch (Exception e) {
            log.warn("Mistral AI is not available: {}", e.getMessage());
            return false;
        }
    }

    private String buildPrompt(String query, String context, ResponseMode mode) {
        return switch (mode) {
            case CHAT, VOICECHAT -> promptTemplateFactory.createChatPrompt(context, query);
            case MINDMAP -> promptTemplateFactory.createMindmapPrompt(context, query);
        };
    }
}
