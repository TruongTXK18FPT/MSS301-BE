package com.mss301.documentservice.service.chunk;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;

import java.util.Map;

public interface PageEstimator {
    void initializeWithTocMapping(Map<Integer, PageMapping> tocPageMapping, int totalPages);
    int estimatePageFromPosition(String fullText, int position);
    int findChunkPosition(String fullText, String chunkText, int startFrom);
}
