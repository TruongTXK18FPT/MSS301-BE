package com.mss301.profileservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.ProfileManagementService;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ProfileManagementService
 * Used by administrators to manage all user profiles
 */
@Slf4j
@Service
@Transactional
public class ProfileManagementServiceImpl implements ProfileManagementService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Override
    public StudentProfileResponse createStudentProfile(String userId, StudentProfileRequest request) {
        log.info("Creating student profile for userId: {}", userId);

        try {
            // Check if user exists
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            // Check if student profile already exists
            if (studentProfileRepository.findByUserId(Long.valueOf(userId)).isPresent()) {
                throw new RuntimeException("Student profile already exists for userId: " + userId);
            }

            // Create student profile
            StudentProfile studentProfile = new StudentProfile();
            studentProfile.setUserId(Long.valueOf(userId));
            studentProfile.setSchool(request.getSchool());
            studentProfile.setGrade(request.getGrade());
            studentProfile.setLearningGoals(request.getLearningGoals());
            studentProfile.setSubjectsOfInterest(request.getSubjectsOfInterest());
            studentProfile.setCreatedAt(LocalDateTime.now());
            studentProfile.setUpdatedAt(LocalDateTime.now());

            studentProfileRepository.save(studentProfile);

            // Return created profile
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
            log.error("Error creating student profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create student profile: " + e.getMessage());
        }
    }

    @Override
    public StudentProfileResponse getStudentProfileByUserId(String userId) {
        log.info("Getting student profile by userId: {}", userId);

        try {
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            StudentProfile studentProfile = studentProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Student profile not found for userId: " + userId));

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
            log.error("Error getting student profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get student profile: " + e.getMessage());
        }
    }

    @Override
    public StudentProfileResponse updateStudentProfile(String userId, StudentProfileRequest request) {
        log.info("Updating student profile for userId: {}", userId);

        try {
            // Update user profile
            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            userProfile.setFullName(request.getFullName());
            userProfile.setPhoneNumber(request.getPhoneNumber());
            userProfile.setDob(LocalDate.parse(request.getBirthDate()));
            userProfile.setAddress(request.getAddress());
            userProfile.setBio(request.getBio());
            userProfile.setUpdatedAt(LocalDateTime.now());

            userProfileRepository.save(userProfile);

            // Update student profile
            StudentProfile studentProfile = studentProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Student profile not found for userId: " + userId));

            studentProfile.setSchool(request.getSchool());
            studentProfile.setGrade(request.getGrade());
            studentProfile.setLearningGoals(request.getLearningGoals());
            studentProfile.setSubjectsOfInterest(request.getSubjectsOfInterest());
            studentProfile.setUpdatedAt(LocalDateTime.now());

            studentProfileRepository.save(studentProfile);

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
            log.error("Error updating student profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update student profile: " + e.getMessage());
        }
    }

    @Override
    public List<StudentProfileResponse> getAllStudentProfiles() {
        log.info("Getting all student profiles");

        try {
            List<StudentProfile> studentProfiles = studentProfileRepository.findAll();

            return studentProfiles.stream()
                    .<StudentProfileResponse>map(studentProfile -> {
                        try {
                            UserProfile userProfile = userProfileRepository.findByUserId(studentProfile.getUserId())
                                    .orElse(null);

                            if (userProfile == null) {
                                return null;
                            }

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
                            log.warn("Failed to build student profile response for userId {}: {}",
                                    studentProfile.getUserId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(profile -> profile != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting all student profiles: {}", e.getMessage());
            throw new RuntimeException("Failed to get all student profiles: " + e.getMessage());
        }
    }
}
