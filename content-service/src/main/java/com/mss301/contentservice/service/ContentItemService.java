package com.mss301.contentservice.service;

import java.util.List;

import com.mss301.contentservice.dto.request.ContentItemRequest;
import com.mss301.contentservice.dto.response.ContentItemResponse;

public interface ContentItemService {
    ContentItemResponse create(ContentItemRequest request, Long ownerId);

    ContentItemResponse update(Long id, ContentItemRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    ContentItemResponse getById(Long id, Long userId);

    List<ContentItemResponse> getMyContents(Long ownerId);

    List<ContentItemResponse> getPublicContents();
}
