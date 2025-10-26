package com.mss301.profileservice.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.profileservice.dto.response.GuardianProfileResponse;
import com.mss301.profileservice.dto.response.GuardianProfileWithStudents;
import com.mss301.profileservice.dto.response.StudentGuardianResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.entity.GuardianProfile;
import com.mss301.profileservice.entity.StudentGuardian;
import com.mss301.profileservice.entity.StudentGuardianVerification;
import com.mss301.profileservice.entity.StudentProfile;
import com.mss301.profileservice.entity.UserProfile;
import com.mss301.profileservice.repository.GuardianProfileRepository;
import com.mss301.profileservice.repository.StudentGuardianRepository;
import com.mss301.profileservice.repository.StudentGuardianVerificationRepository;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.service.GuardianProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of GuardianProfileService
 * Handles guardian-specific profile management and student relationships
 */
@Slf4j
@Service
@Transactional
public class GuardianProfileServiceImpl implements GuardianProfileService {

    @Autowired
    private GuardianProfileRepository guardianProfileRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentGuardianRepository studentGuardianRepository;

    @Autowired
    private StudentGuardianVerificationRepository verificationRepository;

    @Autowired
    private StreamBridge streamBridge;

    @Override
    public GuardianProfileResponse getGuardianProfile(String userId) {
        log.info("Getting guardian profile for userId: {}", userId);

        try {
            GuardianProfile guardianProfile = guardianProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Guardian profile not found for userId: " + userId));

            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            return GuardianProfileResponse.builder()
                    .id(guardianProfile.getId())
                    .userId(guardianProfile.getUserId())
                    .fullName(userProfile.getFullName())
                    .dob(userProfile.getDob())
                    .phoneNumber(userProfile.getPhoneNumber())
                    .address(userProfile.getAddress())
                    .bio(userProfile.getBio())
                    .avatarUrl(userProfile.getAvatarUrl())
                    .relationship(guardianProfile.getRelationship())
                    .phoneAlt(guardianProfile.getPhoneAlt())
                    .createdAt(guardianProfile.getCreatedAt())
                    .updatedAt(guardianProfile.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error getting guardian profile for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get guardian profile: " + e.getMessage());
        }
    }

    @Override
    public GuardianProfileWithStudents getGuardianProfileWithStudents(String userId) {
        log.info("Getting guardian profile with students for userId: {}", userId);

        try {
            GuardianProfile guardianProfile = guardianProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("Guardian profile not found for userId: " + userId));

            UserProfile userProfile = userProfileRepository.findByUserId(Long.valueOf(userId))
                    .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

            // Get associated students
            List<StudentGuardian> relationships = studentGuardianRepository.findByGuardianId(guardianProfile.getId());

            List<StudentProfileResponse> students = relationships.stream()
                    .<StudentProfileResponse>map(rel -> {
                        try {
                            StudentProfile studentProfile = studentProfileRepository.findById(rel.getStudentId())
                                    .orElse(null);

                            if (studentProfile == null) {
                                return null;
                            }

                            // Get UserProfile for student
                            UserProfile studentUserProfile = userProfileRepository
                                    .findByUserId(studentProfile.getUserId())
                                    .orElse(null);

                            if (studentUserProfile == null) {
                                return null;
                            }

                            return StudentProfileResponse.builder()
                                    .userId(studentProfile.getUserId())
                                    .fullName(studentUserProfile.getFullName())
                                    .email(studentUserProfile.getEmail())
                                    .phoneNumber(studentUserProfile.getPhoneNumber())
                                    .dob(studentUserProfile.getDob())
                                    .address(studentUserProfile.getAddress())
                                    .bio(studentUserProfile.getBio())
                                    .avatarUrl(studentUserProfile.getAvatarUrl())
                                    .school(studentProfile.getSchool())
                                    .grade(studentProfile.getGrade())
                                    .createdAt(studentProfile.getCreatedAt())
                                    .updatedAt(studentProfile.getUpdatedAt())
                                    .build();
                        } catch (Exception e) {
                            log.warn("Failed to build student profile for relationship {}: {}", rel, e.getMessage());
                            return null;
                        }
                    })
                    .filter(student -> student != null)
                    .collect(Collectors.toList());

            return GuardianProfileWithStudents.builder()
                    .id(guardianProfile.getId())
                    .userId(guardianProfile.getUserId())
                    .fullName(userProfile.getFullName())
                    .dob(userProfile.getDob())
                    .phoneNumber(userProfile.getPhoneNumber())
                    .address(userProfile.getAddress())
                    .bio(userProfile.getBio())
                    .avatarUrl(userProfile.getAvatarUrl())
                    .relationship(guardianProfile.getRelationship())
                    .phoneAlt(guardianProfile.getPhoneAlt())
                    .createdAt(guardianProfile.getCreatedAt())
                    .updatedAt(guardianProfile.getUpdatedAt())
                    .students(students)
                    .build();

        } catch (Exception e) {
            log.error("Error getting guardian profile with students for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get guardian profile with students: " + e.getMessage());
        }
    }

    @Override
    public List<StudentGuardianResponse> getStudentsByGuardian(String guardianId) {
        log.info("Getting students by guardian ID: {}", guardianId);

        try {
            GuardianProfile guardianProfile = guardianProfileRepository.findById(Long.valueOf(guardianId))
                    .orElseThrow(() -> new RuntimeException("Guardian profile not found for ID: " + guardianId));

            List<StudentGuardian> relationships = studentGuardianRepository.findByGuardianId(guardianProfile.getId());

            return relationships.stream()
                    .<StudentGuardianResponse>map(rel -> {
                        try {
                            StudentProfile studentProfile = studentProfileRepository.findById(rel.getStudentId())
                                    .orElse(null);

                            if (studentProfile == null) {
                                return null;
                            }

                            // Get UserProfile for student
                            UserProfile studentUserProfile = userProfileRepository
                                    .findByUserId(studentProfile.getUserId())
                                    .orElse(null);

                            return StudentGuardianResponse.builder()
                                    .studentId(rel.getStudentId())
                                    .guardianId(rel.getGuardianId())
                                    .student(studentUserProfile != null ? StudentProfileResponse.builder()
                                            .userId(studentProfile.getUserId())
                                            .fullName(studentUserProfile.getFullName())
                                            .email(studentUserProfile.getEmail())
                                            .phoneNumber(studentUserProfile.getPhoneNumber())
                                            .dob(studentUserProfile.getDob())
                                            .address(studentUserProfile.getAddress())
                                            .bio(studentUserProfile.getBio())
                                            .avatarUrl(studentUserProfile.getAvatarUrl())
                                            .school(studentProfile.getSchool())
                                            .grade(studentProfile.getGrade())
                                            .createdAt(studentProfile.getCreatedAt())
                                            .updatedAt(studentProfile.getUpdatedAt())
                                            .build() : null)
                                    .guardian(GuardianProfileResponse.builder()
                                            .id(guardianProfile.getId())
                                            .userId(guardianProfile.getUserId())
                                            .relationship(guardianProfile.getRelationship())
                                            .build())
                                    .build();
                        } catch (Exception e) {
                            log.warn("Failed to build student-guardian response for relationship {}: {}", rel,
                                    e.getMessage());
                            return null;
                        }
                    })
                    .filter(response -> response != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting students by guardian ID {}: {}", guardianId, e.getMessage());
            throw new RuntimeException("Failed to get students by guardian: " + e.getMessage());
        }
    }

    @Override
    public boolean checkGuardianStudentRelationship(String guardianEmail, String studentEmail) {
        log.info("Checking guardian-student relationship: {} -> {}", guardianEmail, studentEmail);

        try {
            // Get guardian profile by email
            UserProfile guardianUser = userProfileRepository.findByEmail(guardianEmail)
                    .orElse(null);
            if (guardianUser == null) {
                return false;
            }

            GuardianProfile guardianProfile = guardianProfileRepository.findByUserId(guardianUser.getUserId())
                    .orElse(null);
            if (guardianProfile == null) {
                return false;
            }

            // Get student profile by email
            UserProfile studentUser = userProfileRepository.findByEmail(studentEmail)
                    .orElse(null);
            if (studentUser == null) {
                return false;
            }

            StudentProfile studentProfile = studentProfileRepository.findByUserId(studentUser.getUserId())
                    .orElse(null);
            if (studentProfile == null) {
                return false;
            }

            // Check relationship
            return studentGuardianRepository.findByStudentIdAndGuardianId(
                    studentProfile.getId(), guardianProfile.getId()).isPresent();

        } catch (Exception e) {
            log.error("Error checking guardian-student relationship {} -> {}: {}", guardianEmail, studentEmail,
                    e.getMessage());
            return false;
        }
    }

    @Override
    public void addStudentToGuardian(String guardianUserId, String studentEmail, String relationship) {
        log.info("Adding student {} to guardian {} with relationship: {}", studentEmail, guardianUserId, relationship);

        try {
            // Get guardian profile
            GuardianProfile guardianProfile = guardianProfileRepository.findByUserId(Long.valueOf(guardianUserId))
                    .orElseThrow(
                            () -> new RuntimeException("Guardian profile not found for userId: " + guardianUserId));

            // Check if student exists
            UserProfile studentUser = userProfileRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found with email: " + studentEmail));

            StudentProfile studentProfile = studentProfileRepository.findByUserId(studentUser.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Student profile not found for userId: " + studentUser.getUserId()));

            // Check if relationship already exists
            if (studentGuardianRepository.findByStudentIdAndGuardianId(studentProfile.getId(), guardianProfile.getId())
                    .isPresent()) {
                throw new RuntimeException("Relationship already exists between guardian and student");
            }

            // Generate verification code (6 digits)
            String verificationCode = String.format("%06d", new Random().nextInt(999999));

            // Create verification record
            StudentGuardianVerification verification = new StudentGuardianVerification();
            verification.setGuardianId(guardianProfile.getId());
            verification.setStudentId(studentProfile.getId());
            verification.setVerificationCode(verificationCode);
            verification.setRelationship(relationship);
            verification.setExpiryTime(java.time.LocalDateTime.now().plusMinutes(5)); // 5 minutes expiry
            verification.setUsed(false);
            verification.setCreatedAt(java.time.LocalDateTime.now());

            verificationRepository.save(verification);

            // Send Guardian Verification OTP email via Kafka event
            try {
                UserProfile guardianUser = userProfileRepository.findByUserId(Long.valueOf(guardianUserId)).get();

                Map<String, Object> guardianEventMap = new HashMap<>();
                guardianEventMap.put("studentEmail", studentEmail);
                guardianEventMap.put("studentName", studentUser.getFullName());
                guardianEventMap.put("guardianName", guardianUser.getFullName());
                guardianEventMap.put("guardianEmail", guardianUser.getEmail());
                guardianEventMap.put("verificationCode", verificationCode);
                guardianEventMap.put("relationship", relationship);
                guardianEventMap.put("expiryMinutes", 5);
                guardianEventMap.put("subject", "Mã Xác Nhận Quan Hệ Phụ Huynh - MathMind");

                streamBridge.send("guardianVerification-out-0", guardianEventMap);
                log.info("Guardian verification OTP email sent to student: {}", studentEmail);
            } catch (Exception e) {
                log.error("Failed to send Guardian verification OTP email to student: {}", studentEmail, e);
                // Don't fail the operation if email sending fails
            }

            // TODO: Send verification email to student
            log.info("Verification code {} sent to student {} for guardian relationship", verificationCode,
                    studentEmail);

        } catch (Exception e) {
            log.error("Error adding student to guardian: {}", e.getMessage());
            throw new RuntimeException("Failed to add student to guardian: " + e.getMessage());
        }
    }

    @Override
    public void verifyStudentRelationship(String guardianUserId, String studentEmail, String verificationCode) {
        log.info("Verifying student relationship: studentEmail={}, code={}", studentEmail, verificationCode);

        try {
            // Get student profile by email
            UserProfile studentUser = userProfileRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found with email: " + studentEmail));

            StudentProfile studentProfile = studentProfileRepository.findByUserId(studentUser.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Student profile not found for userId: " + studentUser.getUserId()));

            // Find verification record by code (unique)
            StudentGuardianVerification verification = verificationRepository
                    .findByVerificationCode(verificationCode)
                    .orElseThrow(() -> new RuntimeException("Invalid verification code"));

            // Get guardian from verification record
            GuardianProfile guardianProfile = guardianProfileRepository.findById(verification.getGuardianId())
                    .orElseThrow(() -> new RuntimeException("Guardian profile not found"));

            // Verify the student matches
            if (!verification.getStudentId().equals(studentProfile.getId())) {
                throw new RuntimeException("Verification code does not match student");
            }

            // Check if verification is expired
            if (verification.getExpiryTime().isBefore(java.time.LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }

            // Check if already used
            if (verification.isUsed()) {
                throw new RuntimeException("Verification code has already been used");
            }

            // Create student-guardian relationship
            StudentGuardian relationship = new StudentGuardian();
            relationship.setGuardianId(guardianProfile.getId());
            relationship.setStudentId(studentProfile.getId());
            relationship.setRelationship(verification.getRelationship());
            // createdAt will be set automatically by @PrePersist

            studentGuardianRepository.save(relationship);

            // Mark verification as used
            verification.setUsed(true);
            verification.setVerifiedAt(java.time.LocalDateTime.now());
            verificationRepository.save(verification);

            log.info("Student relationship verified successfully: guardian={}, student={}", guardianProfile.getId(),
                    studentEmail);

        } catch (Exception e) {
            log.error("Error verifying student relationship: {}", e.getMessage());
            throw new RuntimeException("Failed to verify student relationship: " + e.getMessage());
        }
    }

    @Override
    public void resendGuardianVerification(String guardianUserId, String studentEmail) {
        log.info("Resending guardian verification for student: {}", studentEmail);

        try {
            // Get guardian profile
            GuardianProfile guardianProfile = guardianProfileRepository.findByUserId(Long.valueOf(guardianUserId))
                    .orElseThrow(
                            () -> new RuntimeException("Guardian profile not found for userId: " + guardianUserId));

            // Check if student exists
            UserProfile studentUser = userProfileRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found with email: " + studentEmail));

            StudentProfile studentProfile = studentProfileRepository.findByUserId(studentUser.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Student profile not found for userId: " + studentUser.getUserId()));

            // Check if there's an existing verification record
            Optional<StudentGuardianVerification> existingVerification = verificationRepository
                    .findByGuardianIdAndStudentId(guardianProfile.getId(), studentProfile.getId());

            if (existingVerification.isPresent()) {
                // Update existing verification with new code and expiry
                StudentGuardianVerification verification = existingVerification.get();
                String newVerificationCode = String.format("%06d", new Random().nextInt(999999));

                verification.setVerificationCode(newVerificationCode);
                verification.setExpiryTime(java.time.LocalDateTime.now().plusMinutes(5));
                verification.setUsed(false);
                verification.setCreatedAt(java.time.LocalDateTime.now());

                verificationRepository.save(verification);

                // Send new Guardian Verification OTP email via Kafka event
                try {
                    UserProfile guardianUser = userProfileRepository.findByUserId(Long.valueOf(guardianUserId)).get();

                    Map<String, Object> guardianEventMap = new HashMap<>();
                    guardianEventMap.put("studentEmail", studentEmail);
                    guardianEventMap.put("studentName", studentUser.getFullName());
                    guardianEventMap.put("guardianName", guardianUser.getFullName());
                    guardianEventMap.put("guardianEmail", guardianUser.getEmail());
                    guardianEventMap.put("verificationCode", newVerificationCode);
                    guardianEventMap.put("relationship", verification.getRelationship());
                    guardianEventMap.put("expiryMinutes", 5);
                    guardianEventMap.put("subject", "Mã Xác Nhận Mới - Quan Hệ Phụ Huynh - MathMind");

                    streamBridge.send("guardianVerification-out-0", guardianEventMap);
                    log.info("New Guardian verification OTP email sent to student: {}", studentEmail);
                } catch (Exception e) {
                    log.error("Failed to send new Guardian verification OTP email to student: {}", studentEmail, e);
                    throw new RuntimeException("Failed to send new Guardian verification OTP email: " + e.getMessage());
                }
            } else {
                throw new RuntimeException("No existing verification record found for this student");
            }

        } catch (Exception e) {
            log.error("Error resending guardian verification: {}", e.getMessage());
            throw new RuntimeException("Failed to resend guardian verification: " + e.getMessage());
        }
    }
}
