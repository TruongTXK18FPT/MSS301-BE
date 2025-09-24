package com.mss301.authservice.controller;

import org.springframework.web.bind.annotation.*;

import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.service.AuthenticationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request) {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        authenticationService.verifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Email verified successfully")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Password reset successfully")
                .build();
    }

    @PostMapping("/create-password/{userId}")
    public ApiResponse<Void> createPassword(@PathVariable String userId, @RequestBody PasswordCreationRequest request) {
        authenticationService.createPassword(userId, request);
        return ApiResponse.<Void>builder()
                .message("Password created successfully")
                .build();
    }

    @PostMapping("/send-email-verification")
    public ApiResponse<Void> sendEmailVerification(@RequestParam String email) {
        authenticationService.sendEmailVerification(email);
        return ApiResponse.<Void>builder().message("Verification email sent").build();
    }

    @PostMapping("/send-password-reset")
    public ApiResponse<Void> sendPasswordResetOTP(@RequestParam String email) {
        authenticationService.sendPasswordResetOTP(email);
        return ApiResponse.<Void>builder().message("Password reset OTP sent").build();
    }
}
