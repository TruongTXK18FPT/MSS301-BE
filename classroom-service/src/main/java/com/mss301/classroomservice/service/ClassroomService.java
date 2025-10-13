package com.mss301.classroomservice.service;

import java.util.List;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;

public interface ClassroomService {
    ClassroomResponse create(ClassroomRequest request, Long ownerId);

    ClassroomResponse update(Long id, ClassroomRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    ClassroomResponse getById(Long id, Long userId);

    List<ClassroomResponse> getMyClassrooms(Long ownerId);

    List<ClassroomResponse> getPublicClassrooms();

    String generateJoinCode(Long id, Long ownerId);

    ClassroomResponse joinByCode(String joinCode, Long userId);
}
