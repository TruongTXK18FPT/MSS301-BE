package com.mss301.documentservice.service.integration.impl;

import com.mss301.documentservice.dto.request.EmbeddingRequest;
import com.mss301.documentservice.dto.response.EmbeddingResponse;
import com.mss301.documentservice.service.integration.EmbeddingClient;
import com.mss301.documentservice.service.integration.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingClient embeddingClient;

    @Value("${embedding.client.model:text-embedding-3-small}")
    private String embeddingModel;

    /**
     * Generate embedding for a given text
     */
    public List<Float> generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for embedding generation");
                return new ArrayList<>();
            }

            log.debug("Calling external embedding API for text length: {}", text.length());

            EmbeddingRequest request = new EmbeddingRequest(embeddingModel, text);
            EmbeddingResponse response = embeddingClient.createEmbedding(request);

            if (response.getData() == null || response.getData().isEmpty()) {
                log.error("Empty embedding response");
                return new ArrayList<>();
            }

            List<Double> doubles = response.getData().get(0).getEmbedding();
            List<Float> floats = new ArrayList<>(doubles.size());
            for (Double d : doubles) {
                floats.add(d.floatValue());
            }

            log.debug("Generated embedding with {} dimensions", floats.size());
            return floats;

        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> result = new ArrayList<>();
        for (String text : texts) {
            result.add(generateEmbedding(text));
        }
        return result;
    }

    public boolean isEmbeddingServiceAvailable() {
        try {
            List<Float> test = generateEmbedding("health check");
            return !test.isEmpty();
        } catch (Exception e) {
            log.warn("Embedding service unavailable: {}", e.getMessage());
            return false;
        }
    }
}
