package com.mss301.authservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.TeacherApprovalRequest;
import com.mss301.authservice.dto.request.UpdateUserStatusRequest;
import com.mss301.authservice.dto.request.UserCreationRequest;
import com.mss301.authservice.dto.response.UserResponse;
import com.mss301.authservice.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    // KEPT: Get current user info (used by FE: auth.service.ts)
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    // KEPT: Fetch user by email (potentially useful for admin)
    @GetMapping("/by-email")
    public ApiResponse<UserResponse> getByEmail(@RequestParam("email") String email) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserByEmail(email))
                .build();
    }

    // KEPT: Admin status update (used by FE: admin.service.ts)
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody UpdateUserStatusRequest request) {
        userService.updateUserStatus(id, request);
        return ApiResponse.<Void>builder().build();
    }

    // KEPT: Used by FE: admin.service.ts
    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    // KEPT: Profile completion (used by FE: auth.service.ts)
    @PostMapping("/complete-profile")
    public ApiResponse<String> completeProfile(@RequestBody Object request) {
        userService.completeProfile(request);
        return ApiResponse.<String>builder()
                .result("Profile completed successfully")
                .build();
    }

    // KEPT: Admin endpoint to approve/reject teacher (used by FE: teacher-registrations/page.tsx)
    @PostMapping("/{userId}/teacher-approval")
    public ApiResponse<Void> approveTeacherRegistration(
            @PathVariable Long userId,
            @RequestBody TeacherApprovalRequest request) {
        userService.processTeacherApproval(userId, request);
        return ApiResponse.<Void>builder()
                .message("Teacher registration processed successfully")
                .build();
    }

    // KEPT: Get pending teachers (used by FE: teacher-registrations/page.tsx)
    @GetMapping("/pending-teachers")
    public ApiResponse<List<UserResponse>> getPendingTeachers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getPendingTeachers())
                .build();
    }
}