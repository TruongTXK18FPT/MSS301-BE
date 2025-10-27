package com.mss301.authservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.ProfileStatusResponse;
import com.mss301.authservice.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(UserCreationRequest request);

    UserResponse getUserById(Long id);

    UserResponse getMyInfo();

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);

    Page<UserResponse> getUsers(Pageable pageable);

    List<UserResponse> getAllUsers();

    void updateUserStatus(Long id, UpdateUserStatusRequest request);

    UserResponse getUserByEmail(String email);

    boolean existsByEmail(String email);

    void sendVerificationEmail(Long userId);

    void completeProfile(Object request);

    ProfileStatusResponse getProfileStatus();

    /**
     * Process teacher approval/rejection by admin
     *
     * @param userId  User ID of the teacher
     * @param request Approval request with action and optional rejection reason
     */
    void processTeacherApproval(Long userId, TeacherApprovalRequest request);

    /**
     * Get all pending teacher registrations
     *
     * @return List of pending teacher users
     */
    List<UserResponse> getPendingTeachers();
}
