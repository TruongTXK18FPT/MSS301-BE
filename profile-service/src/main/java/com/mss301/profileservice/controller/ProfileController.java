package com.mss301.profileservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.mss301.profileservice.dto.request.StudentProfileRequest;
import com.mss301.profileservice.dto.response.ApiResponse;
import com.mss301.profileservice.dto.response.StudentProfileResponse;
import com.mss301.profileservice.service.ProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/student")
    public ApiResponse<StudentProfileResponse> createStudentProfile(
            @RequestHeader("X-User-Id") String userId, @RequestBody StudentProfileRequest request) {
        log.info("Creating student profile for user: {}", userId);
        StudentProfileResponse response = profileService.createStudentProfile(userId, request);
        return ApiResponse.success("Student profile created successfully", response);
    }

    @GetMapping("/student")
    public ApiResponse<StudentProfileResponse> getStudentProfile(@RequestHeader("X-User-Id") String userId) {
        log.info("Getting student profile for user: {}", userId);
        StudentProfileResponse response = profileService.getStudentProfileByUserId(userId);
        return ApiResponse.success("Student profile retrieved successfully", response);
    }

    @PutMapping("/student")
    public ApiResponse<StudentProfileResponse> updateStudentProfile(
            @RequestHeader("X-User-Id") String userId, @RequestBody StudentProfileRequest request) {
        log.info("Updating student profile for user: {}", userId);
        StudentProfileResponse response = profileService.updateStudentProfile(userId, request);
        return ApiResponse.success("Student profile updated successfully", response);
    }

    @DeleteMapping("/student")
    public ApiResponse<String> deleteStudentProfile(@RequestHeader("X-User-Id") String userId) {
        log.info("Deleting student profile for user: {}", userId);
        profileService.deleteStudentProfile(userId);
        return ApiResponse.success("Student profile deleted successfully");
    }

    @GetMapping("/students")
    public ApiResponse<List<StudentProfileResponse>> getAllStudentProfiles() {
        log.info("Getting all student profiles");
        List<StudentProfileResponse> responses = profileService.getAllStudentProfiles();
        return ApiResponse.success("Student profiles retrieved successfully", responses);
    }
}
