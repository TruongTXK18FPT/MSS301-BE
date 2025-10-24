package com.mss301.ragservice.strategy.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.ragservice.dto.external.RetrievalResponse;
import com.mss301.ragservice.dto.response.MindmapResponse;
import com.mss301.ragservice.strategy.ResponseStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MindmapResponseStrategy implements ResponseStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object formatResponse(String llmResponse, List<RetrievalResponse.RetrievalResult> results) {
        log.debug("Formatting mindmap response with {} results", results != null ? results.size() : 0);

        List<MindmapResponse.Reference> references = new ArrayList<>();

        if (results != null) {
            for (RetrievalResponse.RetrievalResult result : results) {
                MindmapResponse.Reference reference = MindmapResponse.Reference.builder()
                        .content(result.getContent())
                        .score(result.getScore())
                        .documentId(result.getDocumentId())
                        .chapterId(result.getChapterId())
                        .lessonId(result.getLessonId())
                        .chapterTitle(result.getChapterTitle())
                        .lessonTitle(result.getLessonTitle())
                        .pageNumber(result.getPageNumber())
                        .build();
                references.add(reference);
            }
        }

        return MindmapResponse.builder()
                .mindmapContent(llmResponse)
                .references(references)
                .totalReferences(references.size())
                .build();
    }
}
