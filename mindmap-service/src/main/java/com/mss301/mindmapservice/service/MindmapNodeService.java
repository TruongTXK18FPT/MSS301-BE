package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.MindmapNodeRequest;
import com.mss301.mindmapservice.dto.response.MindmapNodeResponse;

public interface MindmapNodeService {

    /**
     * Create a new node
     */
    MindmapNodeResponse createNode(MindmapNodeRequest request, Long mindmapId, Long userId);

    /**
     * Get node by ID
     */
    MindmapNodeResponse getNodeById(Long id, Long mindmapId, Long userId);

    /**
     * Get all nodes for a mindmap
     */
    List<MindmapNodeResponse> getNodesByMindmapId(Long mindmapId, Long userId);

    /**
     * Update node
     */
    MindmapNodeResponse updateNode(Long id, MindmapNodeRequest request, Long mindmapId, Long userId);

    /**
     * Delete node
     */
    void deleteNode(Long id, Long mindmapId, Long userId);

    /**
     * Move node to new position
     */
    MindmapNodeResponse moveNode(Long id, Double newX, Double newY, Long mindmapId, Long userId);

    /**
     * Update node style
     */
    MindmapNodeResponse updateNodeStyle(Long id, MindmapNodeRequest request, Long mindmapId, Long userId);
}
