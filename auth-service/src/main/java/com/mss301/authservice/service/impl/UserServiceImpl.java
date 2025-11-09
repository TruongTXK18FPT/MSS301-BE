package com.mss301.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.authservice.config.EventPublisher;
import com.mss301.authservice.dto.request.*;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Save teacher-specific registration data as JSON for later use
        if ("TEACHER".equalsIgnoreCase(request.getUserType())) {
            try {
                java.util.Map<String, Object> registrationData = new java.util.HashMap<>();
                registrationData.put("fullName", request.getFullName());
                registrationData.put("department", request.getDepartment());
                registrationData.put("specialization", request.getSpecialization());
                registrationData.put("yearsOfExperience", request.getYearsOfExperience());
                registrationData.put("qualifications", request.getQualifications());
                registrationData.put("bio", request.getBio());
                registrationData.put("phone", request.getPhone());
                
                String jsonData = objectMapper.writeValueAsString(registrationData);
                user.setRegistrationData(jsonData);
                user = userRepository.save(user);
                
                log.info("Saved registration data for teacher: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to save registration data for teacher: {}", user.getEmail(), e);
            }
        }

        try {
            // Send verification email
            authenticationService.sendEmailVerification(user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", user.getEmail(), e);
            // Don't fail the registration if email sending fails
        }

        // NOTE: Profile creation events are now published AFTER email verification
        // See verifyEmail() in AuthenticationServiceImpl for the event publishing logic

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

        // Get user directly without calling getUserById
        UserAccount user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
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

    // KEPT: Used by AdminController
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Page<UserResponse> getUsers(Pageable pageable) {
        // Exclude ADMIN users from admin list view
        return userRepository
                .findNonAdminUsers("ADMIN", pageable)
                .map(this::mapToUserResponse);
    }

    // KEPT: Used by AdminController  
    @PreAuthorize("hasRole('ADMIN')")
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
