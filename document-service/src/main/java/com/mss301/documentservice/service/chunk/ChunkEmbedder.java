package com.mss301.documentservice.service.chunk;

import java.util.List;

public interface ChunkEmbedder {
    Integer estimateTokenCount(String text);

    List<Float> generateEmbedding(String chunkText, int chunkIndex);
}
