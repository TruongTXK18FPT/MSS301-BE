package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.entity.MindmapEdge;
import com.mss301.mindmapservice.entity.MindmapNode;

public interface AiService {

    /**
     * Generate mindmap using AI
     */
    AiGenerateMindmapResponse generateMindmap(AiGenerateMindmapRequest request, Long userId);

    /**
     * Generate mindmap nodes using AI
     */
    List<MindmapNode> generateNodes(Mindmap mindmap, AiGenerateMindmapRequest request);

    /**
     * Generate mindmap edges using AI
     */
    List<MindmapEdge> generateEdges(Mindmap mindmap, List<MindmapNode> nodes, AiGenerateMindmapRequest request);

    /**
     * Get available AI models
     */
    List<String> getAvailableModels(String provider);

    /**
     * Check AI service health
     */
    boolean isServiceHealthy(String provider);
}
