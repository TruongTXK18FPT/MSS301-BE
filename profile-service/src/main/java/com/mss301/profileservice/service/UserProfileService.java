package com.mss301.profileservice.service;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ProfileCompletionStatusResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;

/**
 * Service interface for current user profile operations
 * Handles profile management for the currently authenticated user
 */
public interface UserProfileService {

    /**
     * Get the current user's profile
     *
     * @param userId Current authenticated user ID
     * @return StudentProfileResponse The user's profile
     */
    StudentProfileResponse getCurrentUserProfile(String userId);

    /**
     * Update the current user's profile
     *
     * @param userId  Current authenticated user ID
     * @param request Profile update request
     * @return StudentProfileResponse Updated profile
     */
    StudentProfileResponse updateCurrentUserProfile(String userId, StudentProfileRequest request);

    /**
     * Get profile completion status for the current user
     *
     * @param userId Current authenticated user ID
     * @return ProfileCompletionStatusResponse Profile completion status
     */
    ProfileCompletionStatusResponse getProfileCompletionStatus(String userId);
}
