package com.mss301.ragservice.strategy.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.ragservice.dto.external.RetrievalResponse;
import com.mss301.ragservice.dto.response.ExerciseResponse;
import com.mss301.ragservice.strategy.ResponseStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExerciseResponseStrategy implements ResponseStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object formatResponse(String llmResponse, List<RetrievalResponse.RetrievalResult> results) {
        log.debug("Formatting exercise response with {} results", results != null ? results.size() : 0);

        List<ExerciseResponse.Reference> references = new ArrayList<>();

        if (results != null) {
            for (RetrievalResponse.RetrievalResult result : results) {
                ExerciseResponse.Reference reference = ExerciseResponse.Reference.builder()
                        .content(result.getContent())
                        .score((float) result.getScore())
                        .documentId(result.getDocumentId())
                        .chapterId(result.getChapterId())
                        .lessonId(result.getLessonId())
                        .chapterTitle(result.getChapterTitle())
                        .lessonTitle(result.getLessonTitle())
                        .pageNumber(result.getPageNumber() != null ? result.getPageNumber().intValue() : null)
                        .build();
                references.add(reference);
            }
        }

        return ExerciseResponse.builder()
                .exercisesContent(llmResponse)
                .references(references)
                .totalReferences(references.size())
                .build();
    }
}
