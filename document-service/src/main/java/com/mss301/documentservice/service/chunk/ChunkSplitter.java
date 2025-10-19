package com.mss301.documentservice.service.chunk;

import org.springframework.stereotype.Service;

import java.util.List;

public interface ChunkSplitter {
    List<String> createChunks(String text, int maxChunkSize, int overlap);

    List<String> detectChapters(String text);

    String cleanText(String text);
}
