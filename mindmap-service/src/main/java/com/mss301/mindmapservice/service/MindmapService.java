package com.mss301.mindmapservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.request.MindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapResponse;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;

public interface MindmapService {

    /**
     * Create a new mindmap
     */
    MindmapResponse createMindmap(MindmapRequest request, Long userId);

    /**
     * Generate mindmap using AI
     */
    AiGenerateMindmapResponse generateMindmapWithAi(AiGenerateMindmapRequest request, Long userId);

    /**
     * Get mindmap by ID
     */
    MindmapResponse getMindmapById(Long id, Long userId);

    /**
     * Get nodes for a mindmap
     */
    List<MindmapNodeResponse> getMindmapNodes(Long mindmapId, Long userId);

    /**
     * Get all mindmaps for a user
     */
    List<MindmapResponse> getUserMindmaps(Long userId);

    /**
     * Get public mindmaps
     */
    Page<MindmapResponse> getPublicMindmaps(Pageable pageable);

    /**
     * Search mindmaps
     */
    List<MindmapResponse> searchMindmaps(String keyword, Long userId);

    /**
     * Update mindmap
     */
    MindmapResponse updateMindmap(Long id, MindmapRequest request, Long userId);

    /**
     * Update mindmap with nodes and edges
     */
    MindmapResponse updateMindmapWithNodesAndEdges(Long id, MindmapRequest request, Long userId);

    /**
     * Delete mindmap
     */
    void deleteMindmap(Long id, Long userId);

    /**
     * Share mindmap
     */
    String shareMindmap(Long id, Long userId);

    /**
     * Get shared mindmap by share code
     */
    MindmapResponse getSharedMindmap(String shareCode);

    /**
     * Get user's mindmap statistics
     */
    Object getUserMindmapStats(Long userId);

    /**
     * Check if user can create more mindmaps
     */
    boolean canUserCreateMindmap(Long userId, boolean isPremium);
}
