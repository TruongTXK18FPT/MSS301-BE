package com.mss301.profileservice.service;

import com.mss301.profileservice.dto.response.GuardianProfileResponse;
import com.mss301.profileservice.dto.response.GuardianProfileWithStudents;
import com.mss301.profileservice.dto.response.StudentGuardianResponse;

import java.util.List;

/**
 * Service interface for Guardian profile operations
 * Handles guardian-specific profile management and student relationships
 */
public interface GuardianProfileService {

    /**
     * Get guardian profile by user ID
     *
     * @param userId Guardian user ID
     * @return GuardianProfileResponse Guardian profile
     */
    GuardianProfileResponse getGuardianProfile(String userId);

    /**
     * Get guardian profile with associated students
     *
     * @param userId Guardian user ID
     * @return GuardianProfileWithStudents Guardian profile with students
     */
    GuardianProfileWithStudents getGuardianProfileWithStudents(String userId);

    /**
     * Get all students associated with a guardian
     *
     * @param guardianId Guardian ID
     * @return List of StudentGuardianResponse
     */
    List<StudentGuardianResponse> getStudentsByGuardian(String guardianId);

    /**
     * Add a student to guardian's management list
     * Sends verification email to student for confirmation
     *
     * @param guardianUserId Guardian user ID
     * @param studentEmail   Student email to add
     * @param relationship   Relationship type (Parent, Guardian, etc.)
     */
    void addStudentToGuardian(String guardianUserId, String studentEmail, String relationship);

    /**
     * Verify student relationship after student confirms via email
     *
     * @param guardianUserId   Guardian user ID
     * @param studentEmail     Student email
     * @param verificationCode Verification code from email
     */
    void verifyStudentRelationship(String guardianUserId, String studentEmail, String verificationCode);

    /**
     * Check if guardian-student relationship exists
     *
     * @param guardianEmail Guardian email
     * @param studentEmail  Student email
     * @return boolean True if relationship exists
     */
    boolean checkGuardianStudentRelationship(String guardianEmail, String studentEmail);

    /**
     * Resend guardian verification email to student
     *
     * @param guardianUserId Guardian user ID
     * @param studentEmail   Student email
     */
    void resendGuardianVerification(String guardianUserId, String studentEmail);

}
