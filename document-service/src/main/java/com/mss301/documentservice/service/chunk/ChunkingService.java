package com.mss301.documentservice.service.chunk;

import com.mss301.documentservice.entity.Chunk;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChunkingService {
    List<Chunk> createStructuredChunks(String documentId, String fullText,
                                       int maxChunkSize, int overlap, String language, int totalPages);
}
