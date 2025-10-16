package com.mss301.documentservice.service.chunk.impl;

import com.mss301.documentservice.entity.Chunk;
import com.mss301.documentservice.service.chunk.ChunkingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChunkingServiceImpl implements ChunkingService {
    @Override
    public List<Chunk> createStructuredChunks(String documentId, String fullText, int maxChunkSize, int overlap, String language, int totalPages) {
        return List.of();
    }
}
