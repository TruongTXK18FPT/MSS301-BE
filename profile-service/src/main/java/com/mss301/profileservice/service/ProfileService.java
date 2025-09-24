package com.mss301.profileservice.service;

import java.util.List;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.StudentProfileResponse;

public interface ProfileService {
    StudentProfileResponse createStudentProfile(String userId, StudentProfileRequest request);

    StudentProfileResponse getStudentProfileByUserId(String userId);

    StudentProfileResponse updateStudentProfile(String userId, StudentProfileRequest request);

    void deleteStudentProfile(String userId);

    List<StudentProfileResponse> getAllStudentProfiles();
}
