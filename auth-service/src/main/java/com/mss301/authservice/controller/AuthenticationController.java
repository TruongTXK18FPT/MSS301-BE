package com.mss301.authservice.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.authservice.config.FrontendProperties;
import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.service.AuthenticationService;
import com.mss301.authservice.service.GoogleOAuthService;

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

    @PostMapping("/resend-email-verification")
    public ApiResponse<Void> resendEmailVerificationOTP(@RequestParam String email) {
        authenticationService.resendOTP(email);
        return ApiResponse.<Void>builder().message("Email verification OTP resent successfully").build();
    }

    @PostMapping("/resend-password-reset")
    public ApiResponse<Void> resendPasswordResetOTP(@RequestParam String email) {
        authenticationService.sendPasswordResetOTP(email);
        return ApiResponse.<Void>builder().message("Password reset OTP resent successfully").build();
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
            @RequestParam("code") String code, @RequestParam("state") String state) {
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
            // Log the exception for debugging
            System.err.println("Google OAuth callback error: " + e.getMessage());
            e.printStackTrace();

            // Redirect to error page with specific error message
            String errorMessage = e.getMessage();
            String redirectUrl;

            if (errorMessage.contains("Google login is only available for students")) {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=google_role_restricted&message=" +
                        java.net.URLEncoder.encode(
                                "Đăng nhập Google chỉ dành cho học sinh. Vui lòng sử dụng đăng nhập thường với email và mật khẩu.",
                                java.nio.charset.StandardCharsets.UTF_8);
            } else {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=google_auth_failed&message=" +
                        java.net.URLEncoder.encode("Đăng nhập Google thất bại. Vui lòng thử lại.",
                                java.nio.charset.StandardCharsets.UTF_8);
            }

            System.out.println("Redirecting to: " + redirectUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    @PostMapping("/google/setup-password")
    public ApiResponse<Void> setupPasswordForGoogleUser(@RequestParam String email, @RequestParam String newPassword) {
        authenticationService.setupPasswordForGoogleUser(email, newPassword);
        return ApiResponse.<Void>builder().message("Password setup successful").build();
    }

    @GetMapping("/password-setup-status")
    public ApiResponse<Boolean> getPasswordSetupStatus(@RequestParam String email) {
        boolean passwordSetupRequired = authenticationService.getPasswordSetupStatus(email);
        return ApiResponse.<Boolean>builder()
                .result(passwordSetupRequired)
                .message("Password setup status retrieved successfully")
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Change password request received");
        System.out.println("[DEBUG] Authorization header: " + authHeader);

        // Extract token from Authorization header
        String token = authHeader.replace("Bearer ", "");
        System.out.println("[DEBUG] Extracted token: " + token.substring(0, Math.min(20, token.length())) + "...");

        // Get user info from token
        IntrospectResponse introspectResponse = authenticationService.introspect(new IntrospectRequest(token));
        System.out.println("[DEBUG] Introspect response valid: " + introspectResponse.isValid());

        if (!introspectResponse.isValid()) {
            System.out.println("[DEBUG] Token is invalid, returning 401");
            return ApiResponse.<Void>builder()
                    .code(401)
                    .message("Invalid token")
                    .build();
        }

        // Use authenticated user's email instead of request email
        String userEmail = introspectResponse.getEmail();
        authenticationService.changePassword(userEmail, request.getCurrentPassword(), request.getNewPassword());

        return ApiResponse.<Void>builder()
                .message("Password changed successfully")
                .build();
    }
}
