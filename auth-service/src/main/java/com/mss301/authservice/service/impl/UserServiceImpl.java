package com.mss301.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.config.EventPublisher;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.UserResponse;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.event.CreatedUserEvent;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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
        user.setPhone(request.getPhone());
        user.setEmailVerified(false);
        user.setStatus(UserAccount.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        // Set tenant if provided
        if (request.getTenantId() != null) {
            user.setTenantId(request.getTenantId());
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

    @Override
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

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
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private void publishUserCreatedEvent(UserAccount user, UserCreationRequest request) {
        try {
            // Create CreatedUserEvent with complete user details
            CreatedUserEvent event = CreatedUserEvent.builder()
                    .id(user.getId().toString())
                    .fullName(request.getFullName() != null ? request.getFullName() : request.getUsername())
                    .userType(request.getUserType()) // STUDENT, TEACHER, GUARDIAN
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .districtCode(request.getDistrictCode())
                    .provinceCode(request.getProvinceCode())
                    // birthDate can be added later if needed
                    .build();

            //
            eventPublisher.publishCreatedUserEvent(event);
            log.info("Published CreatedUserEvent for user: {}", user.getEmail());
            //
        } catch (Exception e) {
            log.error("Failed to publish CreatedUserEvent for user: {}", user.getEmail(), e);
        }
    }
}
