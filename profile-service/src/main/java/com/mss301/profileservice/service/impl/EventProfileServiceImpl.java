package com.mss301.profileservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.entity.GuardianProfile;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.TeacherProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.event.CreatedUserEvent;
import com.mss301.profileservice.event.ProfileCompletedEvent;
import com.mss301.profileservice.event.TeacherRegistrationEvent;
import com.mss301.profileservice.event.TeacherApprovalEvent;
import com.mss301.profileservice.repository.GuardianProfileRepository;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.TeacherProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.EventProfileService;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Implementation of EventProfileService
 * Handles automatic profile creation from user events
 */
@Slf4j
@Service
@Transactional
public class EventProfileServiceImpl implements EventProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private GuardianProfileRepository guardianProfileRepository;

    @Override
    public void createProfileFromUserEvent(CreatedUserEvent event) {
        log.info("Creating profile from user event for userId: {}, role: {}", event.getId(), event.getUserType());

        try {
            Long userId = Long.valueOf(event.getId());
            String role = event.getUserType();

            // First, create the base UserProfile
            createUserProfile(userId, event);

            // Then create role-specific profile
            switch (role) {
                case "STUDENT":
                    createStudentProfile(userId, event);
                    break;
                case "TEACHER":
                    createTeacherProfile(userId, event);
                    break;
                case "GUARDIAN":
                    createGuardianProfile(userId, event);
                    break;
                default:
                    log.warn("Unknown role for profile creation: {}", role);
            }
        } catch (Exception e) {
            log.error("Error creating profile from user event: {}", e.getMessage());
            throw new RuntimeException("Failed to create profile from event: " + e.getMessage());
        }
    }

    @Override
    public void createTeacherProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating teacher profile for userId: {}", userId);

        try {
            // Check if teacher profile already exists
            if (teacherProfileRepository.findByUserId(userId).isPresent()) {
                log.info("Teacher profile already exists for userId: {}", userId);
                return;
            }

            TeacherProfile teacherProfile = new TeacherProfile();
            teacherProfile.setUserId(userId);
            teacherProfile.setCreatedAt(LocalDateTime.now());
            teacherProfile.setUpdatedAt(LocalDateTime.now());

            teacherProfileRepository.save(teacherProfile);
            log.info("Teacher profile created successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error creating teacher profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create teacher profile: " + e.getMessage());
        }
    }

    @Override
    public void createTeacherProfileFromEvent(TeacherRegistrationEvent event) {
        log.info("Creating teacher profile from registration event for userId: {}", event.getId());

        try {
            Long userId = Long.valueOf(event.getId());

            // Check if user profile exists, if not create it
            if (!userProfileRepository.findByUserId(userId).isPresent()) {
                createUserProfileFromRegistration(userId, event);
            }

            // Check if teacher profile already exists
            if (teacherProfileRepository.findByUserId(userId).isPresent()) {
                log.info("Teacher profile already exists for userId: {}", userId);
                return;
            }

            // Create teacher profile with all fields from event and PENDING status
            TeacherProfile teacherProfile = new TeacherProfile();
            teacherProfile.setUserId(userId);
            teacherProfile.setDepartment(event.getDepartment());
            teacherProfile.setSpecialization(event.getSpecialization());
            teacherProfile.setYearsOfExperience(event.getYearsOfExperience());
            teacherProfile.setQualifications(event.getQualifications());
            teacherProfile.setBio(event.getBio());
            teacherProfile.setApprovalStatus(TeacherProfile.ApprovalStatus.PENDING); // Start as PENDING
            teacherProfile.setCreatedAt(LocalDateTime.now());
            teacherProfile.setUpdatedAt(LocalDateTime.now());

            teacherProfileRepository.save(teacherProfile);
            log.info("Teacher profile created successfully for userId: {} with PENDING approval status", userId);

        } catch (Exception e) {
            log.error("Error creating teacher profile from event: {}", e.getMessage());
            throw new RuntimeException("Failed to create teacher profile from event: " + e.getMessage());
        }
    }

    private void createUserProfileFromRegistration(Long userId, TeacherRegistrationEvent event) {
        log.info("Creating user profile from registration for userId: {}", userId);

        try {
            // Check if user profile already exists
            if (userProfileRepository.findByUserId(userId).isPresent()) {
                log.info("User profile already exists for userId: {}", userId);
                return;
            }

            UserProfile userProfile = new UserProfile();
            userProfile.setUserId(userId);
            userProfile.setEmail(event.getEmail());
            userProfile.setFullName(event.getFullName());
            userProfile.setPhoneNumber(event.getPhone());
            userProfile.setGoogleUser(false);
            userProfile.setPasswordSetupRequired(false);
            userProfile.setProfileCompleted(false);
            userProfile.setUserType("TEACHER");
            // createdAt and updatedAt will be set automatically by @PrePersist

            userProfileRepository.save(userProfile);
            log.info("User profile created successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error creating user profile from registration for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
    }

    @Override
    public void createGuardianProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating guardian profile for userId: {}", userId);

        try {
            // Check if guardian profile already exists
            if (guardianProfileRepository.findByUserId(userId).isPresent()) {
                log.info("Guardian profile already exists for userId: {}", userId);
                return;
            }

            GuardianProfile guardianProfile = new GuardianProfile();
            guardianProfile.setUserId(userId);
            guardianProfile.setRelationship("Parent"); // Default relationship
            guardianProfile.setCreatedAt(LocalDateTime.now());
            guardianProfile.setUpdatedAt(LocalDateTime.now());

            guardianProfileRepository.save(guardianProfile);
            log.info("Guardian profile created successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error creating guardian profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create guardian profile: " + e.getMessage());
        }
    }

    @Override
    public void completeProfileFromEvent(ProfileCompletedEvent event) {
        log.info("Completing profile from event for userId: {}", event.getUserId());

        try {
            Long userId = Long.valueOf(event.getUserId());
            String role = event.getUserType();

            // Update UserProfile completion status
            UserProfile userProfile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            userProfile.setProfileCompleted(true);
            userProfile.setUserType(role);
            userProfile.setUpdatedAt(LocalDateTime.now());

            userProfileRepository.save(userProfile);

            // Create role-specific profile if not exists
            switch (role) {
                case "STUDENT":
                    if (!studentProfileRepository.findByUserId(userId).isPresent()) {
                        createStudentProfile(userId, null);
                    }
                    break;
                case "TEACHER":
                    if (!teacherProfileRepository.findByUserId(userId).isPresent()) {
                        createTeacherProfile(userId, null);
                    }
                    break;
                case "GUARDIAN":
                    if (!guardianProfileRepository.findByUserId(userId).isPresent()) {
                        createGuardianProfile(userId, null);
                    }
                    break;
            }

            log.info("Profile completed successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error completing profile from event: {}", e.getMessage());
            throw new RuntimeException("Failed to complete profile from event: " + e.getMessage());
        }
    }

    private void createStudentProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating student profile for userId: {}", userId);

        try {
            // Check if student profile already exists
            if (studentProfileRepository.findByUserId(userId).isPresent()) {
                log.info("Student profile already exists for userId: {}", userId);
                return;
            }

            StudentProfile studentProfile = new StudentProfile();
            studentProfile.setUserId(userId);
            // createdAt and updatedAt will be set automatically by @PrePersist

            studentProfileRepository.save(studentProfile);
            log.info("Student profile created successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error creating student profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create student profile: " + e.getMessage());
        }
    }

    /**
     * Create base UserProfile from CreatedUserEvent
     */
    private void createUserProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating user profile for userId: {}", userId);

        try {
            // Check if user profile already exists
            if (userProfileRepository.findByUserId(userId).isPresent()) {
                log.info("User profile already exists for userId: {}", userId);
                return;
            }

            UserProfile userProfile = new UserProfile();
            userProfile.setUserId(userId);
            userProfile.setEmail(event.getEmail());
            userProfile.setFullName(event.getFullName());
            userProfile.setGoogleUser(true); // Default for OAuth users
            userProfile.setPasswordSetupRequired(true); // Default for OAuth users
            userProfile.setProfileCompleted(false); // Will be updated when profile is completed
            userProfile.setUserType(event.getUserType());
            // createdAt and updatedAt will be set automatically by @PrePersist

            userProfileRepository.save(userProfile);
            log.info("User profile created successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error creating user profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
    }

    @Override
    public void updateTeacherApprovalStatus(TeacherApprovalEvent event) {
        log.info("Updating teacher approval status for userId: {} to {}", event.getUserId(), event.getApprovalStatus());

        try {
            Long userId = Long.valueOf(event.getUserId());

            // Find teacher profile
            Optional<TeacherProfile> teacherProfileOpt = teacherProfileRepository.findByUserId(userId);

            if (teacherProfileOpt.isEmpty()) {
                log.warn("Teacher profile not found for userId: {}", userId);
                return;
            }

            TeacherProfile teacherProfile = teacherProfileOpt.get();

            // Update approval status
            if ("APPROVED".equalsIgnoreCase(event.getApprovalStatus())) {
                teacherProfile.setApprovalStatus(TeacherProfile.ApprovalStatus.APPROVED);
                teacherProfile.setRejectionReason(null); // Clear rejection reason
            } else if ("REJECTED".equalsIgnoreCase(event.getApprovalStatus())) {
                teacherProfile.setApprovalStatus(TeacherProfile.ApprovalStatus.REJECTED);
                teacherProfile.setRejectionReason(event.getRejectionReason());
            }

            teacherProfile.setUpdatedAt(LocalDateTime.now());
            teacherProfileRepository.save(teacherProfile);

            log.info("Successfully updated teacher approval status for userId: {} to {}",
                    userId, event.getApprovalStatus());

        } catch (Exception e) {
            log.error("Error updating teacher approval status for userId {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
