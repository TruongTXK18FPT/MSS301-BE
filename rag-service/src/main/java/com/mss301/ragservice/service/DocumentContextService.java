package com.mss301.ragservice.service;

import com.mss301.ragservice.dto.external.RetrievalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentContextService {


    public String buildContext(List<RetrievalResponse.RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            log.warn("No results provided for context building");
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== RELEVANT DOCUMENTS ===\n\n");

        for (int i = 0; i < results.size(); i++) {
            RetrievalResponse.RetrievalResult result = results.get(i);

            contextBuilder.append(String.format("Document %d (Score: %.3f):\n", i + 1, result.getScore()));

            // Add metadata if available
            if (result.getChapterTitle() != null) {
                contextBuilder.append("Chapter: ").append(result.getChapterTitle()).append("\n");
            }
            if (result.getLessonTitle() != null) {
                contextBuilder.append("Lesson: ").append(result.getLessonTitle()).append("\n");
            }
            if (result.getPageNumber() != null) {
                contextBuilder.append("Page: ").append(result.getPageNumber()).append("\n");
            }

            contextBuilder.append("Content:\n");
            contextBuilder.append(result.getContent()).append("\n\n");
            contextBuilder.append("---\n\n");
        }

        String context = contextBuilder.toString();
        log.debug("Built context with {} results, total length: {}", results.size(), context.length());

        return context;
    }
}
