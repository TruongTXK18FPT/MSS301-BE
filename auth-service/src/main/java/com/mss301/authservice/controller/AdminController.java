package com.mss301.authservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.UserCreationRequest;
import com.mss301.authservice.dto.response.UserResponse;
import com.mss301.authservice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User Management", description = "Admin APIs for managing users")
public class AdminController {

    private final UserService userService;

    /**
     * Get all users with pagination
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve all users with pagination")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Admin fetching all users: page={}, size={}, sortBy={}", page, size, sortBy);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<UserResponse> users = userService.getUsers(pageable);

        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .result(users)
                .message("Users retrieved successfully")
                .build());
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.info("Admin fetching user: {}", userId);

        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(user)
                .message("User retrieved successfully")
                .build());
    }

    /**
     * Create new user
     */
    @PostMapping
    @Operation(summary = "Create new user", description = "Admin can create a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreationRequest request) {

        log.info("Admin creating new user: {}", request.getEmail());

        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserResponse>builder()
                .result(user)
                .message("User created successfully")
                .build());
    }

    /**
     * Get all users as list
     */
    @GetMapping("/all")
    @Operation(summary = "Get all users", description = "Retrieve all users as a list")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        log.info("Admin fetching all users");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResponse> users = userService.getUsers(pageable);

        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .result(users)
                .message("All users retrieved successfully")
                .build());
    }
}
