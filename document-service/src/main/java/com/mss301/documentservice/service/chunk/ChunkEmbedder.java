package com.mss301.documentservice.service.chunk;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChunkEmbedder {
    Integer estimateTokenCount(String text);
    List<Float> generateEmbedding(String chunkText, int chunkIndex);
}
