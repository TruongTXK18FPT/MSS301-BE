package com.mss301.authservice.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.authservice.config.FrontendProperties;
import com.mss301.authservice.dto.ApiResponse;
import com.mss301.authservice.dto.request.AuthenticationRequest;
import com.mss301.authservice.dto.request.ChangePasswordRequest;
import com.mss301.authservice.dto.request.IntrospectRequest;
import com.mss301.authservice.dto.request.LogoutRequest;
import com.mss301.authservice.dto.request.RefreshRequest;
import com.mss301.authservice.dto.request.ResetPasswordRequest;
import com.mss301.authservice.dto.request.VerifyEmailRequest;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.dto.response.OTPResponse;
import com.mss301.authservice.dto.response.VerifyEmailResponse;
import com.mss301.authservice.exception.AppException;
import com.mss301.authservice.exception.ErrorCode;
import com.mss301.authservice.service.AuthenticationService;
import com.mss301.authservice.service.GoogleOAuthService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    public ApiResponse<VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        var result = authenticationService.verifyEmail(request);
        return ApiResponse.<VerifyEmailResponse>builder()
                .result(result)
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

    @PostMapping("/send-email-verification")
    public ApiResponse<OTPResponse> sendEmailVerification(
            @RequestParam String email) {
        var result = authenticationService.sendEmailVerification(email);
        return ApiResponse.<OTPResponse>builder()
                .result(result)
                .message("Verification email sent. OTP is valid for 5 minutes.")
                .build();
    }

    @PostMapping("/send-password-reset")
    public ApiResponse<Void> sendPasswordResetOTP(@RequestParam String email) {
        authenticationService.sendPasswordResetOTP(email);
        return ApiResponse.<Void>builder().message("Password reset OTP sent").build();
    }

    @PostMapping("/resend-email-verification")
    public ApiResponse<OTPResponse> resendEmailVerificationOTP(
            @RequestParam String email) {
        var result = authenticationService.sendEmailVerification(email);
        return ApiResponse.<OTPResponse>builder()
                .result(result)
                .message("Email verification OTP resent successfully. OTP is valid for 5 minutes.")
                .build();
    }

    @GetMapping("/otp-info")
    public ApiResponse<OTPResponse> getCurrentOTPInfo(@RequestParam String email) {
        var result = authenticationService.getCurrentOTPInfo(email);
        return ApiResponse.<OTPResponse>builder()
                .result(result)
                .message("OTP info retrieved successfully")
                .build();
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
        } catch (AppException appException) {
            // Handle AppException with specific error codes
            ErrorCode errorCode = appException.getErrorCode();
            String errorMessage = errorCode.getMessage();
            String redirectUrl;

            if (errorCode == ErrorCode.USER_INACTIVE) {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=user_inactive&message=" +
                        URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            } else if (errorCode == ErrorCode.TEACHER_PENDING_APPROVAL) {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=teacher_pending&message=" +
                        URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            } else {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=google_auth_failed&message=" +
                        URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            }

            log.info("Redirecting to: {}", redirectUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            // Log the exception for debugging
            log.error("Google OAuth callback error: {}", e.getMessage(), e);

            // Redirect to error page with specific error message
            String errorMessage = e.getMessage();
            String redirectUrl;

            if (errorMessage != null && errorMessage.contains("Google login is only available for students")) {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=google_role_restricted&message=" +
                        URLEncoder.encode(
                                "Đăng nhập Google chỉ dành cho học sinh. Vui lòng sử dụng đăng nhập thường với email và mật khẩu.",
                                StandardCharsets.UTF_8);
            } else {
                redirectUrl = frontendProperties.getLoginUrl() + "?error=google_auth_failed&message=" +
                        URLEncoder.encode("Đăng nhập Google thất bại. Vui lòng thử lại.",
                                StandardCharsets.UTF_8);
            }

            log.info("Redirecting to: {}", redirectUrl);
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
        log.debug("Change password request received");
        log.debug("Authorization header present: {}", authHeader != null);

        // Extract token from Authorization header
        String token = authHeader.replace("Bearer ", "");
        log.debug("Extracted token (first 20 chars): {}",
                token.length() > 20 ? token.substring(0, 20) + "..." : token);

        // Get user info from token
        IntrospectResponse introspectResponse = authenticationService.introspect(new IntrospectRequest(token));
        log.debug("Introspect response valid: {}", introspectResponse.isValid());

        if (!introspectResponse.isValid()) {
            log.warn("Token is invalid, returning 401");
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
