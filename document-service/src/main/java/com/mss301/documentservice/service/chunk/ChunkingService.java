package com.mss301.documentservice.service.chunk;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.entity.Chunk;

@Service
public interface ChunkingService {
    List<Chunk> createStructuredChunks(
            String documentId, String fullText, int maxChunkSize, int overlap, String language, int totalPages);
}
