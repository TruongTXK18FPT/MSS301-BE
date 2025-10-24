package com.mss301.retrievalservice.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class EmbeddingResponse {
    private String model;
    private List<EmbeddingData> data;
    private EmbeddingUsage usage;

    @Data
    public static class EmbeddingData {
        private int index;
        private String object;
        private List<Double> embedding;
    }

    @Data
    public static class EmbeddingUsage {
        private int total_tokens;
        private int prompt_tokens;
    }
}
