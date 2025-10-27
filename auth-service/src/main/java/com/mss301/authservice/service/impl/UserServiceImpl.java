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
import com.mss301.authservice.event.TeacherApprovalEvent;
import com.mss301.authservice.event.TeacherRegistrationEvent;
import com.mss301.authservice.exception.AppException;
import com.mss301.authservice.exception.ErrorCode;
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
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Create new user
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        // fullName will be set in profile-service, not in auth-service
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

        try {
            // Send verification email
            authenticationService.sendEmailVerification(user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", user.getEmail(), e);
            // Don't fail the registration if email sending fails
        }

        try {
            // Publish appropriate event based on user type
            if ("TEACHER".equalsIgnoreCase(request.getUserType())) {
                // Publish TeacherRegistrationEvent for teachers
                publishTeacherRegistrationEvent(user, request);
            } else {
                // Publish CreatedUserEvent for students and guardians
                publishUserCreatedEvent(user, request);
            }
        } catch (Exception e) {
            log.error("Failed to publish registration event for user: {}", user.getEmail(), e);
            // Don't fail the registration if event publishing fails
        }

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
                throw new AppException(ErrorCode.USER_EXISTED);
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
                    .fullName(request.getFullName()) // Pass fullName from registration
                    .userType(request.getUserType()) // STUDENT, TEACHER, GUARDIAN
                    .build();

            eventPublisher.publishCreatedUserEvent(event);
            log.info("Published CreatedUserEvent for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish CreatedUserEvent for user: {}", user.getEmail(), e);
        }
    }

    private void publishTeacherRegistrationEvent(UserAccount user, UserCreationRequest request) {
        try {
            // Create TeacherRegistrationEvent with teacher-specific details
            TeacherRegistrationEvent event = TeacherRegistrationEvent
                    .builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .fullName(request.getFullName())
                    .department(request.getDepartment())
                    .specialization(request.getSpecialization())
                    .yearsOfExperience(request.getYearsOfExperience())
                    .qualifications(request.getQualifications())
                    .bio(request.getBio())
                    .phone(request.getPhone())
                    .build();

            eventPublisher.publishTeacherRegistrationEvent(event);
            log.info("Published TeacherRegistrationEvent for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish TeacherRegistrationEvent for user: {}", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void completeProfile(Object request) {
        // TODO: Implement profile completion logic
        log.info("Profile completion requested: {}", request);
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
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void processTeacherApproval(Long userId, TeacherApprovalRequest request) {
        log.info("Processing teacher approval for userId: {} with action: {}", userId, request.getAction());

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify this is a teacher
        if (!"TEACHER".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("User is not a teacher");
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            // Approve teacher
            user.setStatus(UserAccount.UserStatus.ACTIVE);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Publish approval event
            publishTeacherApprovalEvent(user, "APPROVED", null);

            log.info("Teacher approved successfully for userId: {}", userId);
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            // Reject teacher
            user.setStatus(UserAccount.UserStatus.INACTIVE);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Publish rejection event
            publishTeacherApprovalEvent(user, "REJECTED", request.getRejectionReason());

            log.info("Teacher rejected for userId: {} with reason: {}", userId, request.getRejectionReason());
        } else {
            throw new RuntimeException("Invalid action. Must be APPROVE or REJECT");
        }
    }

    @Override
    public List<UserResponse> getPendingTeachers() {
        log.info("Getting pending teacher registrations");

        // Get all INACTIVE users with TEACHER role
        List<UserAccount> pendingTeachers = userRepository.findByStatusAndRoleName(
                UserAccount.UserStatus.INACTIVE, "TEACHER");

        return pendingTeachers.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private void publishTeacherApprovalEvent(UserAccount user, String approvalStatus, String rejectionReason) {
        try {
            // Create TeacherApprovalEvent
            TeacherApprovalEvent event = TeacherApprovalEvent
                    .builder()
                    .userId(user.getId().toString())
                    .email(user.getEmail())
                    .approvalStatus(approvalStatus)
                    .rejectionReason(rejectionReason)
                    .build();

            eventPublisher.publishTeacherApprovalEvent(event);
            log.info("Published TeacherApprovalEvent for user: {} with status: {}", user.getEmail(), approvalStatus);
        } catch (Exception e) {
            log.error("Failed to publish TeacherApprovalEvent for user: {}", user.getEmail(), e);
        }
    }
}
