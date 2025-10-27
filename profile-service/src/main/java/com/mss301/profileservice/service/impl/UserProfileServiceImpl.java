package com.mss301.profileservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ProfileCompletionStatusResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.UserProfileService;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of UserProfileService
 * Handles profile management for the currently authenticated user
 */
@Slf4j
@Service
@Primary
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public StudentProfileResponse getCurrentUserProfile(String userId) {
        log.info("Getting current user profile for userId: {}", userId);

        try {
            // Get user profile
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElse(null);

            if (userProfile == null) {
                log.warn("User profile not found for userId: {}, returning empty profile", userId);
                // Return empty profile for users without profile (e.g., Google OAuth users)
                return StudentProfileResponse.builder()
                        .userId(Long.valueOf(userId))
                        .email("")
                        .fullName("")
                        .build();
            }

            // Get student profile if exists
            Optional<StudentProfile> studentProfileOpt = studentProfileRepository.findByUserId(Long.valueOf(userId));

            if (studentProfileOpt.isPresent()) {
                StudentProfile studentProfile = studentProfileOpt.get();
                return StudentProfileResponse.builder()
                        .userId(studentProfile.getUserId())
                        .email(userProfile.getEmail())
                        .fullName(userProfile.getFullName())
                        .dob(userProfile.getDob())
                        .phoneNumber(userProfile.getPhoneNumber())
                        .address(userProfile.getAddress())
                        .bio(userProfile.getBio())
                        .avatarUrl(userProfile.getAvatarUrl())
                        .grade(studentProfile.getGrade())
                        .school(studentProfile.getSchool())
                        .learningGoals(studentProfile.getLearningGoals())
                        .subjectsOfInterest(studentProfile.getSubjectsOfInterest())
                        .isGoogleUser(userProfile.isGoogleUser())
                        .passwordSetupRequired(userProfile.getPasswordSetupRequired())
                        .profileCompleted(userProfile.getProfileCompleted())
                        .userType(userProfile.getUserType())
                        .createdAt(studentProfile.getCreatedAt())
                        .updatedAt(studentProfile.getUpdatedAt())
                        .build();
            } else {
                // Return basic profile without student-specific fields
                return StudentProfileResponse.builder()
                        .userId(userProfile.getUserId())
                        .email(userProfile.getEmail())
                        .fullName(userProfile.getFullName())
                        .dob(userProfile.getDob())
                        .phoneNumber(userProfile.getPhoneNumber())
                        .address(userProfile.getAddress())
                        .bio(userProfile.getBio())
                        .avatarUrl(userProfile.getAvatarUrl())
                        .isGoogleUser(userProfile.isGoogleUser())
                        .passwordSetupRequired(userProfile.getPasswordSetupRequired())
                        .profileCompleted(userProfile.getProfileCompleted())
                        .userType(userProfile.getUserType())
                        .createdAt(userProfile.getCreatedAt())
                        .updatedAt(userProfile.getUpdatedAt())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting current user profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get user profile: " + e.getMessage());
        }
    }

    @Override
    public StudentProfileResponse updateCurrentUserProfile(String userId, StudentProfileRequest request) {
        log.info("Updating current user profile for userId: {}", userId);
        log.info("Request data: fullName={}, birthDate={}, phoneNumber={}, address={}",
                request.getFullName(), request.getBirthDate(), request.getPhoneNumber(), request.getAddress());

        try {
            // Update user profile
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            // Validate required fields
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                throw new RuntimeException("Full name is required");
            }

            // Check both dob and birthDate fields
            String birthDateStr = null;
            if (request.getDob() != null) {
                birthDateStr = request.getDob().toString();
                log.info("Using dob field: {}", birthDateStr);
            } else if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
                birthDateStr = request.getBirthDate();
                log.info("Using birthDate field: {}", birthDateStr);
            } else {
                throw new RuntimeException("Birth date is required");
            }

            userProfile.setFullName(request.getFullName().trim());
            userProfile.setPhoneNumber(request.getPhoneNumber());

            // Parse birth date with error handling
            try {
                userProfile.setDob(LocalDate.parse(birthDateStr));
            } catch (Exception e) {
                log.error("Invalid birth date format: {}", birthDateStr);
                throw new RuntimeException("Invalid birth date format. Expected: yyyy-MM-dd");
            }

            userProfile.setAddress(request.getAddress());
            userProfile.setBio(request.getBio());
            userProfile.setUpdatedAt(LocalDateTime.now());

            log.info("Saving user profile for userId: {}", userId);
            userProfileRepository.save(userProfile);
            log.info("User profile saved successfully");

            // Update or create student profile
            StudentProfile studentProfile = studentProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElse(new StudentProfile());

            studentProfile.setUserId(Long.valueOf(userId));
            studentProfile.setSchool(request.getSchool() != null ? request.getSchool().trim() : null);
            studentProfile.setGrade(request.getGrade() != null ? request.getGrade().trim() : null);
            studentProfile
                    .setLearningGoals(request.getLearningGoals() != null ? request.getLearningGoals().trim() : null);
            studentProfile.setSubjectsOfInterest(
                    request.getSubjectsOfInterest() != null ? request.getSubjectsOfInterest().trim() : null);
            studentProfile.setUpdatedAt(LocalDateTime.now());

            if (studentProfile.getId() == null) {
                studentProfile.setCreatedAt(LocalDateTime.now());
            }

            log.info("Saving student profile for userId: {}", userId);
            studentProfileRepository.save(studentProfile);
            log.info("Student profile saved successfully");

            // Mark profile as completed after successful update
            userProfile.setProfileCompleted(true);
            userProfile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(userProfile);
            log.info("Profile marked as completed for userId: {}", userId);

            // Publish event to sync with auth-service
            try {
                // Create a simple event to notify profile completion
                // This will be handled by auth-service to update UserAccount.profileCompleted
                log.info("Publishing profile completion event for userId: {}", userId);
                // Note: In a real implementation, you would create a proper event class
                // For now, we'll rely on the profile service's getProfileCompletionStatus
                // method
            } catch (Exception e) {
                log.error("Error publishing profile completion event: {}", e.getMessage());
                // Don't fail the operation if event publishing fails
            }

            // Return updated profile
            return StudentProfileResponse.builder()
                    .userId(studentProfile.getUserId())
                    .email(userProfile.getEmail())
                    .fullName(userProfile.getFullName())
                    .dob(userProfile.getDob())
                    .phoneNumber(userProfile.getPhoneNumber())
                    .address(userProfile.getAddress())
                    .bio(userProfile.getBio())
                    .avatarUrl(userProfile.getAvatarUrl())
                    .grade(studentProfile.getGrade())
                    .school(studentProfile.getSchool())
                    .learningGoals(studentProfile.getLearningGoals())
                    .subjectsOfInterest(studentProfile.getSubjectsOfInterest())
                    .isGoogleUser(userProfile.isGoogleUser())
                    .passwordSetupRequired(userProfile.getPasswordSetupRequired())
                    .profileCompleted(userProfile.getProfileCompleted())
                    .userType(userProfile.getUserType())
                    .createdAt(studentProfile.getCreatedAt())
                    .updatedAt(studentProfile.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error updating current user profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        }
    }

    @Override
    public ProfileCompletionStatusResponse getProfileCompletionStatus(String userId) {
        log.info("Getting profile completion status for userId: {}", userId);

        try {
            // Get role from JWT token
            String userType = getRoleFromJwt();
            log.info("User type from JWT: {}", userType);
            
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElse(null);

            if (userProfile == null) {
                log.warn("User profile not found for userId: {}, returning default status", userId);
                // Return default status for users without profile with role from JWT
                return ProfileCompletionStatusResponse.builder()
                        .profileCompleted(false)
                        .userType(userType != null ? userType : "STUDENT") // Use role from JWT
                        .email("")
                        .build();
            }

            // For GUARDIAN and TEACHER roles, profile is always considered completed
            // Only STUDENT role needs profile completion
            boolean isProfileCompleted = userProfile.getProfileCompleted();
            if ("GUARDIAN".equals(userProfile.getUserType()) || "TEACHER".equals(userProfile.getUserType())) {
                isProfileCompleted = true;
            }

            log.info("Profile completion status for userId {}: profileCompleted={}, userType={}, email={}",
                    userId, isProfileCompleted, userProfile.getUserType(), userProfile.getEmail());

            return ProfileCompletionStatusResponse.builder()
                    .profileCompleted(isProfileCompleted)
                    .userType(userProfile.getUserType())
                    .email(userProfile.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Error getting profile completion status for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get profile completion status: " + e.getMessage());
        }
    }

    /**
     * Extract role from JWT token
     */
    private String getRoleFromJwt() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                String role = jwtToken.getToken().getClaimAsString("role");
                log.info("Extracted role from JWT: {}", role);
                return role;
            }
            
            log.warn("Unable to extract role from JWT - authentication is not JwtAuthenticationToken");
            return null;
        } catch (Exception e) {
            log.error("Error extracting role from JWT: {}", e.getMessage());
            return null;
        }
    }
}
