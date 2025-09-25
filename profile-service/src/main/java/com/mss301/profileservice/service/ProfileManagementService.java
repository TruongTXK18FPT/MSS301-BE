package com.mss301.profileservice.service;

import java.util.List;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.StudentProfileResponse;

/**
 * Service interface for administrative profile management operations
 * Used by administrators to manage all user profiles
 */
public interface ProfileManagementService {

    /**
     * Create a new student profile (admin operation)
     *
     * @param userId  User ID for the profile
     * @param request Profile creation request
     * @return StudentProfileResponse Created profile
     */
    StudentProfileResponse createStudentProfile(String userId, StudentProfileRequest request);

    /**
     * Get a specific user's profile by ID (admin operation)
     *
     * @param userId User ID
     * @return StudentProfileResponse The user's profile
     */
    StudentProfileResponse getStudentProfileByUserId(String userId);

    /**
     * Update a specific user's profile (admin operation)
     *
     * @param userId  User ID
     * @param request Profile update request
     * @return StudentProfileResponse Updated profile
     */
    StudentProfileResponse updateStudentProfile(String userId, StudentProfileRequest request);

    /**
     * Get all student profiles (admin operation)
     *
     * @return List<StudentProfileResponse> All student profiles
     */
    List<StudentProfileResponse> getAllStudentProfiles();
}
