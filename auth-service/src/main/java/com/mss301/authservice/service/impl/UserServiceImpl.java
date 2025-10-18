package com.mss301.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.config.EventPublisher;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.ProfileStatusResponse;
import com.mss301.authservice.dto.response.UserResponse;
import com.mss301.authservice.entity.Role;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.event.CreatedUserEvent;
import com.mss301.authservice.event.ProfileCompletedEvent;
import com.mss301.authservice.repository.RoleRepository;
import com.mss301.authservice.repository.UserRepository;
import com.mss301.authservice.service.AuthenticationService;
import com.mss301.authservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        // Create new user
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        // Teacher accounts require admin approval before activation
        if ("TEACHER".equalsIgnoreCase(request.getUserType())) {
            user.setStatus(UserAccount.UserStatus.INACTIVE);
        } else {
            user.setStatus(UserAccount.UserStatus.ACTIVE);
        }
        user.setCreatedAt(LocalDateTime.now());

        // Assign role based on userType
        if (request.getUserType() != null) {
            try {
                Role role = roleRepository
                        .findByName(request.getUserType())
                        .orElseThrow(() -> new RuntimeException("Role not found: " + request.getUserType()));
                user.setRoleId(role.getId());
            } catch (Exception e) {
                log.warn(
                        "Could not assign role for userType: {}. User will be created without role.",
                        request.getUserType());
            }
        }

        user = userRepository.save(user);

        // Send verification email
        authenticationService.sendEmailVerification(user.getEmail());

        // Publish user created event via Kafka
        publishUserCreatedEvent(user, request);

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        UserAccount user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getMyInfo() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getToken().getSubject();
        }

        if (userId == null) {
            throw new RuntimeException("Unauthenticated");
        }

        return getUserById(Long.parseLong(userId));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        UserAccount user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Reset verification status
        }

        // Update password if provided
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserAccount user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserAccount.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ADDED: only admins can list users (aligned with external)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Override
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    // ADDED: only admins can change user status (aligned with external)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Override
    @Transactional
    public void updateUserStatus(Long id, UpdateUserStatusRequest request) {
        UserAccount user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(request.isActive() ? UserAccount.UserStatus.ACTIVE : UserAccount.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        UserAccount user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        return mapToUserResponse(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void sendVerificationEmail(Long userId) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        authenticationService.sendEmailVerification(user.getEmail());
    }

    private UserResponse mapToUserResponse(UserAccount user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .noPassword(user.getPassword() == null)
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified() != null && user.getEmailVerified())
                .tenantId(user.getTenantId() != null ? user.getTenantId().toString() : null)
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private void publishUserCreatedEvent(UserAccount user, UserCreationRequest request) {
        try {
            // Create CreatedUserEvent with basic user details only
            CreatedUserEvent event = CreatedUserEvent.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .fullName(request.getFullName())
                    .username(user.getUsername())
                    .userType(request.getUserType()) // STUDENT, TEACHER, GUARDIAN
                    .build();

            eventPublisher.publishCreatedUserEvent(event);
            log.info("Published CreatedUserEvent for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish CreatedUserEvent for user: {}", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void completeProfile(ProfileCompletionRequest request) {
        // Get current user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getToken().getSubject();
        }

        if (userId == null) {
            throw new RuntimeException("Unauthenticated");
        }

        UserAccount user = userRepository
                .findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate that userType matches
        if (!user.getRole().getName().equalsIgnoreCase(request.getUserType())) {
            throw new RuntimeException("User type mismatch. Cannot change role after registration.");
        }

        // Mark profile as completed
        user.setProfileCompleted(true);
        userRepository.save(user);

        // Publish ProfileCompletedEvent to Kafka
        try {
            ProfileCompletedEvent event = ProfileCompletedEvent.builder()
                    .userId(user.getId().toString())
                    .userType(request.getUserType())
                    .data(request.getData())
                    .build();

            eventPublisher.publishProfileCompletedEvent(event);
            log.info("Published ProfileCompletedEvent for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish ProfileCompletedEvent for user: {}", user.getEmail(), e);
        }
    }

    @Override
    public ProfileStatusResponse getProfileStatus() {
        // Get current user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            userId = jwtToken.getToken().getSubject();
        }

        if (userId == null) {
            throw new RuntimeException("Unauthenticated");
        }

        UserAccount user = userRepository
                .findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ProfileStatusResponse.builder()
                .profileCompleted(user.isProfileCompleted())
                .userType(user.getRole().getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
