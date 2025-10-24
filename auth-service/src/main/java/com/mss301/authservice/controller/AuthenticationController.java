package com.mss301.authservice.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.service.AuthenticationService;
import com.mss301.authservice.service.GoogleOAuthService;
import com.mss301.authservice.config.FrontendProperties;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;
    GoogleOAuthService googleOAuthService;
    FrontendProperties frontendProperties;

    @PostMapping("/login")
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

    @GetMapping("/google/redirect")
    public ResponseEntity<Void> redirectToGoogle() {
        try {
            String state = UUID.randomUUID().toString();
            String authorizationUrl = googleOAuthService.generateAuthorizationUrl(state);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", authorizationUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        try {
            // Process Google OAuth callback
            AuthenticationResponse authResponse = authenticationService.authenticateWithGoogle(code);

            // Determine redirect URL based on auth response
            String redirectUrl;
            if (authResponse.isAuthenticated() && authResponse.getToken() != null) {
                // User authenticated successfully - redirect with token for localStorage
                redirectUrl = frontendProperties.getDashboardUrl() + "?token=" + authResponse.getToken();
            } else {
                // New user needs password setup
                redirectUrl = frontendProperties.getPasswordSetupUrl() + "?email=" + authResponse.getEmail();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            // Redirect to error page
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", frontendProperties.getLoginUrl() + "?error=google_auth_failed");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    @PostMapping("/google/setup-password")
    public ApiResponse<Void> setupPasswordForGoogleUser(@RequestParam String email, @RequestParam String newPassword) {
        authenticationService.setupPasswordForGoogleUser(email, newPassword);
        return ApiResponse.<Void>builder().message("Password setup successful").build();
    }

}
