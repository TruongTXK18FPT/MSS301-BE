package com.mss301.documentservice.service.chunk.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.chunk.ChunkEmbedder;
import com.mss301.documentservice.service.integration.EmbeddingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkEmbedderImpl implements ChunkEmbedder {

    private EmbeddingService embeddingService;

    @Value("${chunking.embedding.log.enabled:false}")
    private boolean embeddingLogEnabled;

    @Value("${chunking.embedding.log.interval:100}")
    private int logInterval;

    @Override
    public Integer estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    @Override
    public List<Float> generateEmbedding(String chunkText, int chunkIndex) {
        try {
            if (embeddingLogEnabled && (chunkIndex % logInterval == 0)) {
                log.debug("Generating embedding for chunk {} (length: {})", chunkIndex, chunkText.length());
            }

            List<Float> embedding = embeddingService.generateEmbedding(chunkText);

            if (embeddingLogEnabled && (chunkIndex < 5 || chunkIndex % logInterval == 0)) {
                log.debug(
                        "Successfully generated embedding with {} dimensions for chunk {}",
                        embedding.size(),
                        chunkIndex);
            }

            return embedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding for chunk {}: {}", chunkIndex, e.getMessage());
            return new ArrayList<>();
        }
    }
}
