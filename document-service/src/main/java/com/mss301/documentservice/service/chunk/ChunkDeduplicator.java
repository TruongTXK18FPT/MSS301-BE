package com.mss301.documentservice.service.chunk;

import java.util.List;

public interface ChunkDeduplicator {
    List<String> createChunksWithDeduplication(String text, int maxChunkSize, int overlap);
}
