package com.mss301.profileservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "APIs for managing current user profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(
            summary = "Get Current User Profile",
            description = "Get the profile of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    public ResponseEntity<StudentProfileResponse> getCurrentUserProfile() {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = profileService.getCurrentUserProfile(currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update Current User Profile",
            description = "Update the profile of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    public ResponseEntity<StudentProfileResponse> updateCurrentUserProfile(@RequestBody StudentProfileRequest request) {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = profileService.updateCurrentUserProfile(currentUserId, request);
        return ResponseEntity.ok(response);
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
