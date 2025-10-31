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

    @Value("classpath:prompts/exercise-prompt.st")
    private Resource exercisePromptResource;

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

    public String createExercisePrompt(String context, String queryText) {
        log.debug("Creating exercise prompt");

        try {
            String template = loadTemplate(exercisePromptResource);

            // Parse parameters from queryText
            // Expected format: "Generate N exercises about 'topic' with difficulty 'X' and cognitive level 'Y'"
            int numberOfExercises = extractNumber(queryText, "Generate ", " exercises");
            String topic = extractQuoted(queryText, "about '", "'");
            String difficulty = extractQuoted(queryText, "difficulty level '", "'");
            String cognitiveLevel = extractQuoted(queryText, "cognitive level '", "'");

            PromptTemplate promptTemplate = new PromptTemplate(template);
            return promptTemplate.render(Map.of(
                    "context", context,
                    "topic", topic != null ? topic : "mathematics",
                    "numberOfExercises", String.valueOf(numberOfExercises > 0 ? numberOfExercises : 3),
                    "difficulty", difficulty != null ? difficulty : "MEDIUM",
                    "cognitiveLevel", cognitiveLevel != null ? cognitiveLevel : "COMPREHENSION"));

        } catch (Exception e) {
            log.error("Error creating exercise prompt", e);
            throw new RuntimeException("Failed to create exercise prompt", e);
        }
    }

    private int extractNumber(String text, String start, String end) {
        try {
            int startIdx = text.indexOf(start);
            if (startIdx < 0) return 3;
            startIdx += start.length();
            int endIdx = text.indexOf(end, startIdx);
            if (endIdx < 0) return 3;
            return Integer.parseInt(text.substring(startIdx, endIdx).trim());
        } catch (Exception e) {
            return 3;
        }
    }

    private String extractQuoted(String text, String start, String end) {
        try {
            int startIdx = text.indexOf(start);
            if (startIdx < 0) return null;
            startIdx += start.length();
            int endIdx = text.indexOf(end, startIdx);
            if (endIdx < 0) return null;
            return text.substring(startIdx, endIdx);
        } catch (Exception e) {
            return null;
        }
    }

    private String loadTemplate(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
