package com.mss301.authservice.service;

import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.entity.UserAccount;

/**
 * Service interface for Google User operations
 * Follows Single Responsibility Principle (SRP)
 */
public interface GoogleUserService {

    /**
     * Create a new Google user
     *
     * @param userInfo Google user information
     * @param userType User type (STUDENT, TEACHER, GUARDIAN)
     * @return Created UserAccount
     */
    UserAccount createGoogleUser(GoogleUserInfoResponse userInfo, String userType);

    /**
     * Check if user exists by email
     *
     * @param email User email
     * @return true if user exists, false otherwise
     */
    boolean userExists(String email);

    /**
     * Get user by email
     *
     * @param email User email
     * @return UserAccount if exists, null otherwise
     */
    UserAccount getUserByEmail(String email);

    /**
     * Update user last login time
     *
     * @param user UserAccount to update
     */
    void updateLastLogin(UserAccount user);
}
