package com.mss301.profileservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.ProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public StudentProfileResponse createStudentProfile(String userId, StudentProfileRequest request) {
        log.info("Creating student profile for user: {}", userId);

        try {
            // Check if profile already exists
            Optional<StudentProfile> existingProfile = studentProfileRepository.findByUserId(Long.valueOf(userId));
            if (existingProfile.isPresent()) {
                log.warn("Student profile already exists for user: {}", userId);
                throw new RuntimeException("Student profile already exists for this user");
            }

            // Create or get UserProfile
            UserProfile userProfile = userProfileRepository
                    .findByUserId(Long.valueOf(userId))
                    .orElseGet(() -> {
                        UserProfile newProfile = new UserProfile();
                        newProfile.setUserId(Long.valueOf(userId));
                        newProfile.setFullName(request.getFullName());
                        newProfile.setDob(request.getDob());
                        newProfile.setPhoneNumber(request.getPhoneNumber());
                        newProfile.setAddress(request.getAddress());
                        newProfile.setCreatedAt(LocalDateTime.now());
                        newProfile.setUpdatedAt(LocalDateTime.now());
                        return userProfileRepository.save(newProfile);
                    });

            // Create StudentProfile
            StudentProfile studentProfile = new StudentProfile();
            studentProfile.setUserId(Long.valueOf(userId));
            studentProfile.setUserProfile(userProfile);
            studentProfile.setGrade(request.getGrade());
            studentProfile.setSchool(request.getSchool());
            studentProfile.setLearningGoals(request.getLearningGoals());
            studentProfile.setSubjectsOfInterest(request.getSubjectsOfInterest());
            studentProfile.setCreatedAt(LocalDateTime.now());
            studentProfile.setUpdatedAt(LocalDateTime.now());

            StudentProfile savedProfile = studentProfileRepository.save(studentProfile);
            log.info("Student profile created successfully for user: {}", userId);

            return mapToStudentProfileResponse(savedProfile);

        } catch (Exception e) {
            log.error("Failed to create student profile for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create student profile: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfileByUserId(String userId) {
        log.info("Fetching student profile for user: {}", userId);

        try {
            StudentProfile profile = studentProfileRepository
                    .findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Student profile not found for user: " + userId));

            log.info("Student profile fetched successfully for user: {}", userId);
            return mapToStudentProfileResponse(profile);

        } catch (Exception e) {
            log.error("Failed to fetch student profile for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch student profile: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public StudentProfileResponse updateStudentProfile(String userId, StudentProfileRequest request) {
        log.info("Updating student profile for user: {}", userId);

        try {
            StudentProfile existingProfile = studentProfileRepository
                    .findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Student profile not found for user: " + userId));

            // Update UserProfile if it exists
            if (existingProfile.getUserProfile() != null) {
                UserProfile userProfile = existingProfile.getUserProfile();
                userProfile.setFullName(request.getFullName());
                userProfile.setDob(request.getDob());
                userProfile.setPhoneNumber(request.getPhoneNumber());
                userProfile.setAddress(request.getAddress());
                userProfile.setUpdatedAt(LocalDateTime.now());
                userProfileRepository.save(userProfile);
            }

            // Update StudentProfile
            existingProfile.setGrade(request.getGrade());
            existingProfile.setSchool(request.getSchool());
            existingProfile.setLearningGoals(request.getLearningGoals());
            existingProfile.setSubjectsOfInterest(request.getSubjectsOfInterest());
            existingProfile.setUpdatedAt(LocalDateTime.now());

            StudentProfile updatedProfile = studentProfileRepository.save(existingProfile);
            log.info("Student profile updated successfully for user: {}", userId);

            return mapToStudentProfileResponse(updatedProfile);

        } catch (Exception e) {
            log.error("Failed to update student profile for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update student profile: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteStudentProfile(String userId) {
        log.info("Deleting student profile for user: {}", userId);

        try {
            StudentProfile profile = studentProfileRepository
                    .findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Student profile not found for user: " + userId));

            studentProfileRepository.delete(profile);
            log.info("Student profile deleted successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to delete student profile for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete student profile: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentProfileResponse> getAllStudentProfiles() {
        log.info("Fetching all student profiles");

        try {
            List<StudentProfile> profiles = studentProfileRepository.findAll();
            log.info("Fetched {} student profiles", profiles.size());

            return profiles.stream().map(this::mapToStudentProfileResponse).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch all student profiles: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch student profiles: " + e.getMessage());
        }
    }

    private StudentProfileResponse mapToStudentProfileResponse(StudentProfile profile) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setGrade(profile.getGrade());
        response.setSchool(profile.getSchool());
        response.setLearningGoals(profile.getLearningGoals());
        response.setSubjectsOfInterest(profile.getSubjectsOfInterest());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        // Map UserProfile data if available
        if (profile.getUserProfile() != null) {
            UserProfile userProfile = profile.getUserProfile();
            response.setFullName(userProfile.getFullName());
            response.setDob(userProfile.getDob());
            response.setPhoneNumber(userProfile.getPhoneNumber());
            response.setAddress(userProfile.getAddress());
        }

        return response;
    }
}
