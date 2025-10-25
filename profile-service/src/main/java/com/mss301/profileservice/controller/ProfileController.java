package com.mss301.profileservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ApiResponse;
import com.mss301.profileservice.dto.response.ProfileCompletionStatusResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.service.ProfileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getCurrentUserProfile() {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = profileService.getCurrentUserProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateCurrentUserProfile(
            @RequestBody StudentProfileRequest request) {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = profileService.updateCurrentUserProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/completion-status")
    public ResponseEntity<ApiResponse<ProfileCompletionStatusResponse>> getProfileCompletionStatus() {
        String currentUserId = getCurrentUserId();
        ProfileCompletionStatusResponse response = profileService.getProfileCompletionStatus(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Extract the current user ID from JWT authentication token
     */
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String userId = jwtToken.getToken().getSubject();
            if (userId != null) {
                return userId;
            }
        }

        throw new RuntimeException("Unable to determine current user ID");
    }
}
