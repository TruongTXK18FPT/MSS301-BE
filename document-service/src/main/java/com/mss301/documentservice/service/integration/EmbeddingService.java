package com.mss301.documentservice.service.integration;

import java.util.List;

public interface EmbeddingService {
    List<Float> generateEmbedding(String text);
}
