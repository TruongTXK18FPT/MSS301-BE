package com.mss301.contentservice.service;

import com.mss301.contentservice.dto.request.AssignmentDetailRequest;
import com.mss301.contentservice.dto.response.AssignmentDetailResponse;

public interface AssignmentService {

    AssignmentDetailResponse getAssignment(Long contentItemId, Long userId);

    AssignmentDetailResponse putAssignment(Long contentItemId, AssignmentDetailRequest request, Long userId);
}
