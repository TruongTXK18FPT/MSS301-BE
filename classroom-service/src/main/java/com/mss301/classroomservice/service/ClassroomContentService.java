package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.ClassroomContentRequest;
import com.mss301.classroomservice.dto.response.ClassroomContentResponse;

public interface ClassroomContentService {
    ClassroomContentResponse attach(Long classroomId, ClassroomContentRequest request, Long userId);

    void detach(Long classroomId, Long contentLinkId, Long userId);

    List<ClassroomContentResponse> list(Long classroomId, Long userId);
}
