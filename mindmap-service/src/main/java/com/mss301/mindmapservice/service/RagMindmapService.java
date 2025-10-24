package com.mss301.mindmapservice.service;

import com.mss301.mindmapservice.dto.request.RagMindmapRequest;
import com.mss301.mindmapservice.dto.response.RagMindmapResponse;

public interface RagMindmapService {

    /**
     * Generate mindmap using RAG with document context
     */
    RagMindmapResponse generateRagMindmap(RagMindmapRequest request, Long userId);

    /**
     * Get relevant documents for mindmap generation
     */
    String getRelevantDocuments(RagMindmapRequest request);

    /**
     * Check if RAG service is available
     */
    boolean isRagServiceAvailable();
}
