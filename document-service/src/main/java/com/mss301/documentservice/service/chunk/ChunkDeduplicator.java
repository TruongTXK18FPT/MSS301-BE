package com.mss301.documentservice.service.chunk;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;

import java.util.List;
import java.util.Map;

public interface ChunkDeduplicator {
    List<String> createChunksWithDeduplication(String text, int maxChunkSize, int overlap);
}
