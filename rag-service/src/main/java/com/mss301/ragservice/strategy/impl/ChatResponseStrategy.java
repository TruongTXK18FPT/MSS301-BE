package com.mss301.ragservice.strategy.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mss301.ragservice.dto.external.RetrievalResponse;
import com.mss301.ragservice.dto.response.ChatResponse;
import com.mss301.ragservice.strategy.ResponseStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChatResponseStrategy implements ResponseStrategy {

    @Override
    public Object formatResponse(String llmResponse, List<RetrievalResponse.RetrievalResult> results) {
        log.debug("Formatting chat response with {} results", results != null ? results.size() : 0);

        // Build sources from results
        List<ChatResponse.Source> sources = new ArrayList<>();

        if (results != null) {
            for (RetrievalResponse.RetrievalResult result : results) {
                ChatResponse.Source source = ChatResponse.Source.builder()
                        .content(result.getContent())
                        .score(result.getScore())
                        .documentId(result.getDocumentId())
                        .chapterId(result.getChapterId())
                        .lessonId(result.getLessonId())
                        .chapterTitle(result.getChapterTitle())
                        .lessonTitle(result.getLessonTitle())
                        .pageNumber(result.getPageNumber())
                        .build();
                sources.add(source);
            }
        }

        return ChatResponse.builder()
                .answer(llmResponse)
                .sources(sources)
                .totalSources(sources.size())
                .build();
    }
}
