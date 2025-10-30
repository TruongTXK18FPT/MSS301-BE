package com.mss301.profileservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mss301.profileservice.dto.response.TeacherProfileResponse;
import com.mss301.profileservice.entity.TeacherProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.repository.TeacherProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.TeacherProfileService;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Service implementation for teacher profile operations
 */
@Slf4j
@Service
public class TeacherProfileServiceImpl implements TeacherProfileService {

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Override
    public TeacherProfileResponse getTeacherProfileByUserId(Long userId) {
        log.info("Getting teacher profile for userId: {}", userId);

        try {
            // Get teacher profile
            Optional<TeacherProfile> teacherProfileOpt = teacherProfileRepository.findByUserId(userId);

            if (teacherProfileOpt.isEmpty()) {
                log.warn("Teacher profile not found for userId: {}", userId);
                return null;
            }

            TeacherProfile teacherProfile = teacherProfileOpt.get();

            // Get user profile for additional info
            Optional<UserProfile> userProfileOpt = userProfileRepository.findByUserId(userId);

            // Build response
            TeacherProfileResponse response = TeacherProfileResponse.builder()
                    .id(teacherProfile.getId())
                    .userId(teacherProfile.getUserId())
                    .department(teacherProfile.getDepartment())
                    .specialization(teacherProfile.getSpecialization())
                    .yearsOfExperience(teacherProfile.getYearsOfExperience())
                    .qualifications(teacherProfile.getQualifications())
                    .bio(teacherProfile.getBio())
                    .createdAt(teacherProfile.getCreatedAt())
                    .updatedAt(teacherProfile.getUpdatedAt())
                    .build();

            // Add user profile info if exists
            if (userProfileOpt.isPresent()) {
                UserProfile userProfile = userProfileOpt.get();
                response.setFullName(userProfile.getFullName());
                response.setDob(userProfile.getDob());
                response.setPhoneNumber(userProfile.getPhoneNumber());
                response.setAddress(userProfile.getAddress());
                response.setEmail(userProfile.getEmail());
            }

            // Set approval status
            response.setApprovalStatus(
                    teacherProfile.getApprovalStatus() != null ? teacherProfile.getApprovalStatus().name() : null);
            response.setRejectionReason(teacherProfile.getRejectionReason());

            log.info("Successfully retrieved teacher profile for userId: {}", userId);
            return response;

        } catch (Exception e) {
            log.error("Error getting teacher profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get teacher profile: " + e.getMessage());
        }
    }

    @Override
    public TeacherProfileResponse getTeacherProfileById(Long profileId) {
        log.info("Getting teacher profile for profileId: {}", profileId);

        try {
            Optional<TeacherProfile> teacherProfileOpt = teacherProfileRepository.findById(profileId);

            if (teacherProfileOpt.isEmpty()) {
                log.warn("Teacher profile not found for profileId: {}", profileId);
                throw new RuntimeException("Teacher profile not found");
            }

            TeacherProfile teacherProfile = teacherProfileOpt.get();

            // Get user profile for additional info
            Optional<UserProfile> userProfileOpt = userProfileRepository.findByUserId(teacherProfile.getUserId());

            // Build response
            TeacherProfileResponse response = TeacherProfileResponse.builder()
                    .id(teacherProfile.getId())
                    .userId(teacherProfile.getUserId())
                    .department(teacherProfile.getDepartment())
                    .specialization(teacherProfile.getSpecialization())
                    .yearsOfExperience(teacherProfile.getYearsOfExperience())
                    .qualifications(teacherProfile.getQualifications())
                    .bio(teacherProfile.getBio())
                    .createdAt(teacherProfile.getCreatedAt())
                    .updatedAt(teacherProfile.getUpdatedAt())
                    .build();

            // Add user profile info if exists
            if (userProfileOpt.isPresent()) {
                UserProfile userProfile = userProfileOpt.get();
                response.setFullName(userProfile.getFullName());
                response.setDob(userProfile.getDob());
                response.setPhoneNumber(userProfile.getPhoneNumber());
                response.setAddress(userProfile.getAddress());
                response.setEmail(userProfile.getEmail());
            }

            // Set approval status
            response.setApprovalStatus(
                    teacherProfile.getApprovalStatus() != null ? teacherProfile.getApprovalStatus().name() : null);
            response.setRejectionReason(teacherProfile.getRejectionReason());

            log.info("Successfully retrieved teacher profile for profileId: {}", profileId);
            return response;

        } catch (Exception e) {
            log.error("Error getting teacher profile for profileId {}: {}", profileId, e.getMessage());
            throw new RuntimeException("Failed to get teacher profile: " + e.getMessage());
        }
    }
}
