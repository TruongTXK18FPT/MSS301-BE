package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.ClassroomContentRequest;
import com.mss301.classroomservice.dto.response.ClassroomContentResponse;

public interface ClassroomContentService {
    ClassroomContentResponse attach(Long classroomId, ClassroomContentRequest request, Long userId);

    void detach(Long classroomId, Long contentLinkId, Long userId);

    List<ClassroomContentResponse> list(Long classroomId, Long userId);
    
    // List only visible contents for students (publishAt <= now, visible=true)
    List<ClassroomContentResponse> listVisibleContents(Long classroomId, Long userId);
    
    // CRUD operations for lesson content
    ClassroomContentResponse createContent(Long classroomId, ClassroomContentRequest request, Long userId);
    
    ClassroomContentResponse updateContent(Long contentId, ClassroomContentRequest request, Long userId);
    
    void deleteContent(Long contentId, Long userId);
}
