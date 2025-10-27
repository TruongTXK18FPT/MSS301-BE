package com.mss301.profileservice.service;

import com.mss301.profileservice.dto.response.TeacherProfileResponse;

/**
 * Service interface for teacher profile operations
 */
public interface TeacherProfileService {

    /**
     * Get teacher profile by user ID
     *
     * @param userId User ID
     * @return TeacherProfileResponse
     */
    TeacherProfileResponse getTeacherProfileByUserId(Long userId);

    /**
     * Get teacher profile by teacher profile ID
     *
     * @param profileId Teacher profile ID
     * @return TeacherProfileResponse
     */
    TeacherProfileResponse getTeacherProfileById(Long profileId);
}
