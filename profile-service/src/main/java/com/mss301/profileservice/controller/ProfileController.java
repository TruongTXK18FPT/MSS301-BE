package com.mss301.profileservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ApiResponse;
import com.mss301.profileservice.dto.response.ProfileCompletionStatusResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.dto.response.GuardianProfileResponse;
import com.mss301.profileservice.dto.response.GuardianProfileWithStudents;
import com.mss301.profileservice.dto.response.StudentGuardianResponse;
import com.mss301.profileservice.service.UserProfileService;
import com.mss301.profileservice.service.GuardianProfileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;
    private final GuardianProfileService guardianProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getCurrentUserProfile() {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = userProfileService.getCurrentUserProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateCurrentUserProfile(
            @RequestBody StudentProfileRequest request) {
        String currentUserId = getCurrentUserId();
        StudentProfileResponse response = userProfileService.updateCurrentUserProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/completion-status")
    public ResponseEntity<ApiResponse<ProfileCompletionStatusResponse>> getProfileCompletionStatus() {
        String currentUserId = getCurrentUserId();
        ProfileCompletionStatusResponse response = userProfileService.getProfileCompletionStatus(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Guardian Profile Endpoints
    @GetMapping("/guardian/me")
    public ResponseEntity<ApiResponse<GuardianProfileResponse>> getGuardianProfile() {
        String currentUserId = getCurrentUserId();
        GuardianProfileResponse response = guardianProfileService.getGuardianProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/guardian/me/students")
    public ResponseEntity<ApiResponse<GuardianProfileWithStudents>> getGuardianProfileWithStudents() {
        String currentUserId = getCurrentUserId();
        GuardianProfileWithStudents response = guardianProfileService.getGuardianProfileWithStudents(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/guardian/{guardianId}/students")
    public ResponseEntity<ApiResponse<java.util.List<StudentGuardianResponse>>> getStudentsByGuardian(
            @PathVariable String guardianId) {
        java.util.List<StudentGuardianResponse> response = guardianProfileService.getStudentsByGuardian(guardianId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/guardian/add-student")
    public ResponseEntity<ApiResponse<Void>> addStudentToGuardian(
            @RequestParam String studentEmail,
            @RequestParam String relationship) {
        String currentUserId = getCurrentUserId();
        guardianProfileService.addStudentToGuardian(currentUserId, studentEmail, relationship);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/guardian/verify-student")
    public ResponseEntity<ApiResponse<Void>> verifyStudentRelationship(
            @RequestParam String studentEmail,
            @RequestParam String verificationCode) {
        String currentUserId = getCurrentUserId();
        guardianProfileService.verifyStudentRelationship(currentUserId, studentEmail, verificationCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/guardian/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendGuardianVerification(
            @RequestParam String studentEmail) {
        String currentUserId = getCurrentUserId();
        guardianProfileService.resendGuardianVerification(currentUserId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success(null));
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
