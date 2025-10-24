package com.mss301.authservice.service;

import com.mss301.authservice.dto.request.PasswordSetupRequest;
import com.mss301.authservice.entity.UserAccount;

/**
 * Service interface for password setup operations
 * Follows Single Responsibility Principle (SRP)
 */
public interface PasswordSetupService {

    /**
     * Setup password for Google user
     *
     * @param request Password setup request
     * @return Updated UserAccount
     */
    UserAccount setupPassword(PasswordSetupRequest request);

    /**
     * Check if user needs password setup
     *
     * @param email User email
     * @return true if password setup required, false otherwise
     */
    boolean isPasswordSetupRequired(String email);

    /**
     * Validate password setup request
     *
     * @param request Password setup request
     * @return true if valid, false otherwise
     */
    boolean validatePasswordSetup(PasswordSetupRequest request);
}
