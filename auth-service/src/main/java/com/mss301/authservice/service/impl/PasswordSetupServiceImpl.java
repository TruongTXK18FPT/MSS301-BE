package com.mss301.authservice.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.dto.request.PasswordSetupRequest;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.repository.UserRepository;
import com.mss301.authservice.service.PasswordSetupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Password Setup Service
 * Follows Single Responsibility Principle (SRP) and Dependency Inversion
 * Principle (DIP)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordSetupServiceImpl implements PasswordSetupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserAccount setupPassword(PasswordSetupRequest request) {
        try {
            log.info("Setting up password for user: {}", request.getEmail());

            // Validate request
            if (!validatePasswordSetup(request)) {
                throw new IllegalArgumentException("Invalid password setup request");
            }

            // Find user
            UserAccount user = userRepository
                    .findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user is Google user
            if (!user.getIsGoogleUser()) {
                throw new IllegalArgumentException("User is not a Google user");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setPasswordSetupRequired(false);

            UserAccount updatedUser = userRepository.save(user);
            log.info("Successfully setup password for user: {}", updatedUser.getEmail());

            return updatedUser;

        } catch (Exception e) {
            log.error("Failed to setup password: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to setup password: " + e.getMessage());
        }
    }

    @Override
    public boolean isPasswordSetupRequired(String email) {
        try {
            UserAccount user = userRepository.findByEmail(email).orElse(null);

            return user != null && user.getIsGoogleUser() && user.getPasswordSetupRequired();

        } catch (Exception e) {
            log.error("Failed to check password setup requirement: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean validatePasswordSetup(PasswordSetupRequest request) {
        try {
            // Check if passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password confirmation does not match");
                return false;
            }

            // Check password strength
            if (request.getNewPassword().length() < 6) {
                log.warn("Password is too short");
                return false;
            }

            // Check if email is provided
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.warn("Email is required");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to validate password setup: {}", e.getMessage(), e);
            return false;
        }
    }
}
