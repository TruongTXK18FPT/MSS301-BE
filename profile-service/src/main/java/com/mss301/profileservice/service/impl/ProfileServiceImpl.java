package com.mss301.profileservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.config.EventPublisher;
import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ProfileCompletionStatusResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.entity.GuardianProfile;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.TeacherProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.event.CreatedUserEvent;
import com.mss301.profileservice.event.ProfileCompletedEvent;
import com.mss301.profileservice.event.UserProfileCreationFailedEvent;
import com.mss301.profileservice.repository.GuardianProfileRepository;
import com.mss301.profileservice.repository.StudentGuardianRepository;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.TeacherProfileRepository;
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
    private final TeacherProfileRepository teacherProfileRepository;
    private final GuardianProfileRepository guardianProfileRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final EventPublisher eventPublisher;
    // Note: using repositories above to link guardian and student

    // UserProfileService implementation - Current user operations
    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getCurrentUserProfile(String userId) {
        log.info("Fetching current user profile for user: {}", userId);
        return getStudentProfileByUserId(userId);
    }

    @Override
    @Transactional
    public StudentProfileResponse updateCurrentUserProfile(String userId, StudentProfileRequest request) {
        log.info("Updating current user profile for user: {}", userId);
        return updateStudentProfile(userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileCompletionStatusResponse getProfileCompletionStatus(String userId) {
        log.info("Getting profile completion status for user: {}", userId);

        UserProfile userProfile = userProfileRepository
                .findByUserId(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User profile not found for user ID: " + userId));

        return ProfileCompletionStatusResponse.builder()
                .profileCompleted(userProfile.isProfileCompleted())
                .userType(userProfile.getUserType())
                .username(userProfile.getUsername())
                .email(userProfile.getEmail())
                .build();
    }

    // ProfileManagementService implementation - Admin operations

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

    @Override
    @Transactional
    public void createProfileFromUserEvent(CreatedUserEvent event) {
        log.info("Creating profile from event for user ID: {} with type: {}", event.getId(), event.getUserType());

        try {
            Long userId = Long.valueOf(event.getId());

            // Create empty base UserProfile only (no role-specific profiles yet)
            // User will complete their profile later via ProfileCompletedEvent
            createBaseUserProfile(userId, event);

            log.info(
                    "Empty profile creation completed for user ID: {} with type: {}",
                    event.getId(),
                    event.getUserType());

        } catch (Exception e) {
            log.error("Failed to create profile from event for user ID: {}: {}", event.getId(), e.getMessage(), e);

            // Publish failure event
            UserProfileCreationFailedEvent failureEvent = UserProfileCreationFailedEvent.builder()
                    .userId(event.getId())
                    .reason("Failed to create profile: " + e.getMessage())
                    .build();

            eventPublisher.publishUserProfileCreationFailedEvent(failureEvent);
        }
    }

    private void createBaseUserProfile(Long userId, CreatedUserEvent event) {
        // Check if user profile already exists
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            log.info("User profile already exists for user ID: {}", userId);
            return;
        }

        // Create new empty UserProfile with only basic info from registration
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);
        userProfile.setEmail(event.getEmail());
        userProfile.setFullName(event.getFullName());
        userProfile.setUsername(event.getUsername());
        userProfile.setUserType(event.getUserType());
        userProfile.setProfileCompleted(false); // Default to false, will be set to true when profile is completed
        userProfile.setCreatedAt(LocalDateTime.now());
        userProfile.setUpdatedAt(LocalDateTime.now());

        userProfileRepository.save(userProfile);
        log.info("Empty base user profile created for user ID: {} with type: {}", userId, event.getUserType());
    }

    private void autoCreateStudentProfile(Long userId) {
        if (studentProfileRepository.existsByUserId(userId)) {
            return;
        }
        StudentProfile studentProfile = new StudentProfile();
        studentProfile.setUserId(userId);
        // link back to base user profile
        userProfileRepository.findByUserId(userId).ifPresent(studentProfile::setUserProfile);
        studentProfile.setCreatedAt(LocalDateTime.now());
        studentProfile.setUpdatedAt(LocalDateTime.now());
        studentProfileRepository.save(studentProfile);
    }

    // tryLinkGuardianToStudent() method removed - guardian linking now happens
    // during profile completion phase, not
    // registration

    @Override
    @Transactional
    public void createTeacherProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating teacher profile for user ID: {}", userId);

        // Check if teacher profile already exists
        if (teacherProfileRepository.existsByUserId(userId)) {
            log.info("Teacher profile already exists for user ID: {}", userId);
            return;
        }

        TeacherProfile teacherProfile = new TeacherProfile();
        teacherProfile.setUserId(userId);
        // Set default values or fields from event if available
        teacherProfile.setDepartment("General"); // Default department
        // Additional teacher-specific fields can be set here

        teacherProfileRepository.save(teacherProfile);
        log.info("Teacher profile created for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void createGuardianProfile(Long userId, CreatedUserEvent event) {
        log.info("Creating guardian profile for user ID: {}", userId);

        // Check if guardian profile already exists
        if (guardianProfileRepository.existsByUserId(userId)) {
            log.info("Guardian profile already exists for user ID: {}", userId);
            return;
        }

        GuardianProfile guardianProfile = new GuardianProfile();
        guardianProfile.setUserId(userId);
        guardianProfile.setRelationship("Parent"); // Default relationship

        guardianProfileRepository.save(guardianProfile);
        log.info("Guardian profile created for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void completeProfileFromEvent(ProfileCompletedEvent event) {
        log.info("Completing profile from event for user ID: {} with type: {}", event.getUserId(), event.getUserType());

        try {
            Long userId = Long.valueOf(event.getUserId());

            // Get and update UserProfile
            UserProfile userProfile = userProfileRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found for user ID: " + userId));

            // Mark profile as completed
            userProfile.setProfileCompleted(true);
            userProfile.setUpdatedAt(LocalDateTime.now());

            // Process role-specific data
            if (event.getData() != null && event.getUserType() != null) {
                switch (event.getUserType().toUpperCase()) {
                    case "STUDENT":
                        completeStudentProfile(userId, userProfile, event.getData());
                        break;
                    case "TEACHER":
                        completeTeacherProfile(userId, userProfile, event.getData());
                        break;
                    case "GUARDIAN":
                        completeGuardianProfile(userId, userProfile, event.getData());
                        break;
                    default:
                        log.warn("Unknown user type: {} for user ID: {}", event.getUserType(), userId);
                }
            }

            userProfileRepository.save(userProfile);
            log.info("Profile completion successful for user ID: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to complete profile for user ID: {}: {}", event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to complete profile: " + e.getMessage(), e);
        }
    }

    private void completeStudentProfile(Long userId, UserProfile userProfile, Object data) {
        log.info("Completing student profile for user ID: {}", userId);

        try {
            // Parse data as Map to extract fields
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> studentData = (java.util.Map<String, Object>) data;

            // Update UserProfile with common fields
            if (studentData.containsKey("phone")) {
                userProfile.setPhoneNumber((String) studentData.get("phone"));
            }
            if (studentData.containsKey("birthDate")) {
                String birthDateStr = (String) studentData.get("birthDate");
                if (birthDateStr != null && !birthDateStr.isEmpty()) {
                    userProfile.setDob(java.time.LocalDate.parse(birthDateStr));
                }
            }

            // Create or update StudentProfile
            StudentProfile studentProfile = studentProfileRepository
                    .findByUserId(userId)
                    .orElseGet(() -> {
                        StudentProfile newProfile = new StudentProfile();
                        newProfile.setUserId(userId);
                        newProfile.setUserProfile(userProfile);
                        newProfile.setCreatedAt(LocalDateTime.now());
                        return newProfile;
                    });

            if (studentData.containsKey("school")) {
                studentProfile.setSchool((String) studentData.get("school"));
            }
            if (studentData.containsKey("grade")) {
                studentProfile.setGrade((String) studentData.get("grade"));
            }
            if (studentData.containsKey("learningGoals")) {
                studentProfile.setLearningGoals((String) studentData.get("learningGoals"));
            }
            if (studentData.containsKey("subjectsOfInterest")) {
                studentProfile.setSubjectsOfInterest((String) studentData.get("subjectsOfInterest"));
            }

            studentProfile.setUpdatedAt(LocalDateTime.now());
            studentProfileRepository.save(studentProfile);

            log.info("Student profile completed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error completing student profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to complete student profile", e);
        }
    }

    private void completeTeacherProfile(Long userId, UserProfile userProfile, Object data) {
        log.info("Completing teacher profile for user ID: {}", userId);

        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> teacherData = (java.util.Map<String, Object>) data;

            // Update UserProfile with common fields
            if (teacherData.containsKey("phone")) {
                userProfile.setPhoneNumber((String) teacherData.get("phone"));
            }
            if (teacherData.containsKey("birthDate")) {
                String birthDateStr = (String) teacherData.get("birthDate");
                if (birthDateStr != null && !birthDateStr.isEmpty()) {
                    userProfile.setDob(java.time.LocalDate.parse(birthDateStr));
                }
            }
            if (teacherData.containsKey("bio")) {
                userProfile.setBio((String) teacherData.get("bio"));
            }

            // Create or update TeacherProfile
            TeacherProfile teacherProfile = teacherProfileRepository
                    .findByUserId(userId)
                    .orElseGet(() -> {
                        TeacherProfile newProfile = new TeacherProfile();
                        newProfile.setUserId(userId);
                        newProfile.setUserProfile(userProfile);
                        newProfile.setCreatedAt(LocalDateTime.now());
                        return newProfile;
                    });

            if (teacherData.containsKey("department")) {
                teacherProfile.setDepartment((String) teacherData.get("department"));
            }
            if (teacherData.containsKey("specialization")) {
                teacherProfile.setSpecialization((String) teacherData.get("specialization"));
            }
            if (teacherData.containsKey("yearsOfExperience")) {
                Object yearsExp = teacherData.get("yearsOfExperience");
                if (yearsExp instanceof Number) {
                    teacherProfile.setYearsOfExperience(((Number) yearsExp).intValue());
                }
            }
            if (teacherData.containsKey("qualifications")) {
                teacherProfile.setQualifications((String) teacherData.get("qualifications"));
            }

            teacherProfile.setUpdatedAt(LocalDateTime.now());
            teacherProfileRepository.save(teacherProfile);

            log.info("Teacher profile completed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error completing teacher profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to complete teacher profile", e);
        }
    }

    private void completeGuardianProfile(Long userId, UserProfile userProfile, Object data) {
        log.info("Completing guardian profile for user ID: {}", userId);

        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> guardianData = (java.util.Map<String, Object>) data;

            // Update UserProfile with common fields
            if (guardianData.containsKey("phone")) {
                userProfile.setPhoneNumber((String) guardianData.get("phone"));
            }
            if (guardianData.containsKey("birthDate")) {
                String birthDateStr = (String) guardianData.get("birthDate");
                if (birthDateStr != null && !birthDateStr.isEmpty()) {
                    userProfile.setDob(java.time.LocalDate.parse(birthDateStr));
                }
            }

            // Create or update GuardianProfile
            GuardianProfile guardianProfile = guardianProfileRepository
                    .findByUserId(userId)
                    .orElseGet(() -> {
                        GuardianProfile newProfile = new GuardianProfile();
                        newProfile.setUserId(userId);
                        return newProfile;
                    });

            if (guardianData.containsKey("relationship")) {
                guardianProfile.setRelationship((String) guardianData.get("relationship"));
            }

            guardianProfileRepository.save(guardianProfile);

            // Handle student linking via email + phone validation
            if (guardianData.containsKey("studentEmail") && guardianData.containsKey("studentPhone")) {
                String studentEmail = (String) guardianData.get("studentEmail");
                String studentPhone = (String) guardianData.get("studentPhone");

                linkGuardianToStudent(userId, studentEmail, studentPhone);
            }

            log.info("Guardian profile completed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error completing guardian profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to complete guardian profile", e);
        }
    }

    private void linkGuardianToStudent(Long guardianUserId, String studentEmail, String studentPhone) {
        log.info("Attempting to link guardian {} to student with email: {}", guardianUserId, studentEmail);

        try {
            // Find student by email
            UserProfile studentUserProfile = userProfileRepository
                    .findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found with email: " + studentEmail));

            // Validate phone number matches
            if (studentUserProfile.getPhoneNumber() == null
                    || !studentUserProfile.getPhoneNumber().equals(studentPhone)) {
                throw new RuntimeException("Student phone number does not match for email: " + studentEmail);
            }

            // Validate student has STUDENT role
            if (!"STUDENT".equalsIgnoreCase(studentUserProfile.getUserType())) {
                throw new RuntimeException("User with email " + studentEmail + " is not a student");
            }

            // Find StudentProfile
            StudentProfile studentProfile = studentProfileRepository
                    .findByUserId(studentUserProfile.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Student profile not found for user ID: " + studentUserProfile.getUserId()));

            // Find GuardianProfile
            GuardianProfile guardianProfile = guardianProfileRepository
                    .findByUserId(guardianUserId)
                    .orElseThrow(
                            () -> new RuntimeException("Guardian profile not found for user ID: " + guardianUserId));

            // Create StudentGuardian link
            com.mss301.profileservice.entity.StudentGuardian link =
                    new com.mss301.profileservice.entity.StudentGuardian();
            link.setStudentId(studentProfile.getId());
            link.setGuardianId(guardianProfile.getId());

            studentGuardianRepository.save(link);

            log.info("Successfully linked guardian {} to student {}", guardianUserId, studentProfile.getUserId());
        } catch (Exception e) {
            log.error(
                    "Failed to link guardian {} to student with email {}: {}",
                    guardianUserId,
                    studentEmail,
                    e.getMessage());
            throw new RuntimeException("Failed to link guardian to student: " + e.getMessage(), e);
        }
    }
}
