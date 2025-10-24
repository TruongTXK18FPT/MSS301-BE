package com.mss301.ragservice.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PromptTemplateFactory {

    @Value("classpath:prompts/chat-prompt.st")
    private Resource chatPromptResource;

    @Value("classpath:prompts/mindmap-prompt.st")
    private Resource mindmapPromptResource;

    public String createChatPrompt(String context, String question) {
        log.debug("Creating chat prompt");

        try {
            String template = loadTemplate(chatPromptResource);

            PromptTemplate promptTemplate = new PromptTemplate(template);
            return promptTemplate.render(Map.of(
                    "context", context,
                    "question", question));

        } catch (Exception e) {
            log.error("Error creating chat prompt", e);
            throw new RuntimeException("Failed to create chat prompt", e);
        }
    }

    public String createMindmapPrompt(String context, String question) {
        log.debug("Creating mindmap prompt");

        try {
            String template = loadTemplate(mindmapPromptResource);

            PromptTemplate promptTemplate = new PromptTemplate(template);
            return promptTemplate.render(Map.of(
                    "context", context,
                    "question", question));

        } catch (Exception e) {
            log.error("Error creating mindmap prompt", e);
            throw new RuntimeException("Failed to create mindmap prompt", e);
        }
    }

    private String loadTemplate(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
