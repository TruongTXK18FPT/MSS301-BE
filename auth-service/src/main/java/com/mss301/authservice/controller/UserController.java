package com.mss301.authservice.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.TeacherApprovalRequest;
import com.mss301.authservice.dto.request.UpdateUserStatusRequest;
import com.mss301.authservice.dto.request.UserCreationRequest;
import com.mss301.authservice.dto.request.UserUpdateRequest;
import com.mss301.authservice.dto.response.ProfileStatusResponse;
import com.mss301.authservice.dto.response.UserResponse;
import com.mss301.authservice.service.AuthenticationService;
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
    AuthenticationService authenticationService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOTP(@RequestParam String email) {
        authenticationService.resendOTP(email);
        return ApiResponse.<Void>builder()
                .message("OTP đã được gửi lại thành công")
                .build();
    }

    @GetMapping("/get-users")
    public ApiResponse<Page<UserResponse>> getUsers(Pageable pageable) {
        return ApiResponse.<Page<UserResponse>>builder()
                .result(userService.getUsers(pageable))
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") Long userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    // ADDED: alias endpoint mirroring external "me" pattern, reusing your existing
    // service
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    // ADDED: fetch by email (adapted from external) without duplicating service
    // logic
    @GetMapping("/by-email")
    public ApiResponse<UserResponse> getByEmail(@RequestParam("email") String email) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserByEmail(email))
                .build();
    }

    // ADDED: admin-only status toggle/update adapted from external
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody UpdateUserStatusRequest request) {
        userService.updateUserStatus(id, request);
        return ApiResponse.<Void>builder().build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PostMapping("/complete-profile")
    public ApiResponse<String> completeProfile(@RequestBody Object request) {
        userService.completeProfile(request);
        return ApiResponse.<String>builder()
                .result("Profile completed successfully")
                .build();
    }

    @GetMapping("/my-profile-status")
    public ApiResponse<ProfileStatusResponse> getProfileStatus() {
        return ApiResponse.<ProfileStatusResponse>builder()
                .result(userService.getProfileStatus())
                .build();
    }

    // Admin endpoint to approve/reject teacher registration
    @PostMapping("/{userId}/teacher-approval")
    public ApiResponse<Void> approveTeacherRegistration(
            @PathVariable Long userId,
            @RequestBody TeacherApprovalRequest request) {
        userService.processTeacherApproval(userId, request);
        return ApiResponse.<Void>builder()
                .message("Teacher registration processed successfully")
                .build();
    }

    // Admin endpoint to get pending teacher registrations
    @GetMapping("/pending-teachers")
    public ApiResponse<List<UserResponse>> getPendingTeachers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getPendingTeachers())
                .build();
    }
}
