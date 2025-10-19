package com.mss301.documentservice.service.chunk;

import java.util.List;

import org.springframework.stereotype.Service;

public interface ChunkEmbedder {
    Integer estimateTokenCount(String text);

    List<Float> generateEmbedding(String chunkText, int chunkIndex);
}
