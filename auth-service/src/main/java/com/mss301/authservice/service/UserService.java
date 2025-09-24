package com.mss301.authservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mss301.authservice.dto.request.*;
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
}
