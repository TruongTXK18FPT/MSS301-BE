package com.mss301.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.entity.Role;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.repository.RoleRepository;
import com.mss301.authservice.repository.UserRepository;
import com.mss301.authservice.service.GoogleUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Google User Service
 * Follows Single Responsibility Principle (SRP) and Dependency Inversion
 * Principle (DIP)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleUserServiceImpl implements GoogleUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // PasswordEncoder not needed for Google users initially

    @Override
    @Transactional
    public UserAccount createGoogleUser(GoogleUserInfoResponse userInfo, String userType) {
        try {
            log.info("Creating Google user for email: {}", userInfo.getEmail());

            // Create new Google user
            UserAccount user = new UserAccount();
            user.setEmail(userInfo.getEmail());
            // fullName will be set in profile-service, not in auth-service
            user.setGoogleId(userInfo.getId());
            user.setIsGoogleUser(true);
            user.setPasswordSetupRequired(true); // Google users need to setup password
            user.setEmailVerified(userInfo.getVerifiedEmail() != null ? userInfo.getVerifiedEmail() : true);
            user.setStatus(UserAccount.UserStatus.ACTIVE);
            user.setProfileCompleted(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Set role based on user type
            Long roleId = getUserRoleId(userType);
            user.setRoleId(roleId);
            log.info("Set roleId {} for Google user: {}", roleId, userInfo.getEmail());

            UserAccount savedUser = userRepository.save(user);
            log.info("Successfully created Google user: {}", savedUser.getEmail());

            return savedUser;

        } catch (Exception e) {
            log.error("Failed to create Google user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Google user: " + e.getMessage());
        }
    }

    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserAccount getUserByEmail(String email) {
        Optional<UserAccount> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    @Override
    @Transactional
    public void updateLastLogin(UserAccount user) {
        try {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Updated last login for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to update last login: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update last login: " + e.getMessage());
        }
    }

    /**
     * Get role ID based on user type from database
     *
     * @param userType User type
     * @return Role ID
     */
    private Long getUserRoleId(String userType) {
        try {
            log.info("Looking for role with name: {}", userType.toUpperCase());
            Optional<Role> role = roleRepository.findByName(userType.toUpperCase());
            if (role.isPresent()) {
                log.info("Found role: {} with ID: {}", role.get().getName(), role.get().getId());
                return role.get().getId();
            } else {
                log.warn("Role not found for userType: {}, defaulting to STUDENT", userType);
                // Default to STUDENT if role not found
                Optional<Role> defaultRole = roleRepository.findByName("STUDENT");
                if (defaultRole.isPresent()) {
                    log.info("Using default STUDENT role with ID: {}", defaultRole.get().getId());
                    return defaultRole.get().getId();
                } else {
                    throw new RuntimeException("Default STUDENT role not found");
                }
            }
        } catch (Exception e) {
            log.error("Error getting role ID for userType: {}", userType, e);
            throw new RuntimeException("Failed to get role ID: " + e.getMessage());
        }
    }
}
