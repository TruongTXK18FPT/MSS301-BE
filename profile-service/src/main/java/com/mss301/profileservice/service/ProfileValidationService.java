package com.mss301.profileservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.mss301.profileservice.repository.UserProfileRepository;
import com.mss301.profileservice.repository.StudentProfileRepository;
import com.mss301.profileservice.repository.GuardianProfileRepository;

/**
 * Service for application-level relationship validation
 * Implements business rules for entity relationships
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileValidationService {

    private final UserProfileRepository userProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GuardianProfileRepository guardianProfileRepository;

    /**
     * Validates that a user profile exists
     * 
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if user profile doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateUserProfileExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (!userProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("User profile not found for ID: " + userId);
        }

        log.debug("User profile validation passed for ID: {}", userId);
    }

    /**
     * Validates that a student profile exists
     * 
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if student profile doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateStudentProfileExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (!studentProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Student profile not found for ID: " + userId);
        }

        log.debug("Student profile validation passed for ID: {}", userId);
    }

    /**
     * Validates that a guardian profile exists
     * 
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if guardian profile doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateGuardianProfileExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (!guardianProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Guardian profile not found for ID: " + userId);
        }

        log.debug("Guardian profile validation passed for ID: {}", userId);
    }

    /**
     * Validates that both student and guardian profiles exist before linking
     * 
     * @param studentUserId  the student user ID
     * @param guardianUserId the guardian user ID
     * @throws IllegalArgumentException if either profile doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateStudentGuardianLink(Long studentUserId, Long guardianUserId) {
        validateStudentProfileExists(studentUserId);
        validateGuardianProfileExists(guardianUserId);

        // Additional business rule: student and guardian cannot be the same person
        if (studentUserId.equals(guardianUserId)) {
            throw new IllegalArgumentException("Student and guardian cannot be the same person");
        }

        log.debug("Student-guardian link validation passed for student: {}, guardian: {}",
                studentUserId, guardianUserId);
    }
}
