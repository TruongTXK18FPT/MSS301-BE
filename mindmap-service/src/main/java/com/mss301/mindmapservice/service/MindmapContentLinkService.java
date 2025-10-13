package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.MindmapContentLinkRequest;
import com.mss301.mindmapservice.dto.response.MindmapContentLinkResponse;

public interface MindmapContentLinkService {
    MindmapContentLinkResponse attach(Long mindmapId, MindmapContentLinkRequest request, Long userId);

    void detach(Long mindmapId, Long linkId, Long userId);

    List<MindmapContentLinkResponse> list(Long mindmapId, Long userId);
}
