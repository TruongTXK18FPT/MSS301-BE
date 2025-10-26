package com.mss301.authservice.service;

import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request);

    void logout(LogoutRequest request);

    AuthenticationResponse refreshToken(RefreshRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void resetPassword(ResetPasswordRequest request);

    void createPassword(String userId, PasswordCreationRequest request);

    void sendEmailVerification(String email);

    void sendPasswordResetOTP(String email);

    void resendOTP(String email);

    /**
     * Authenticate user using Google OAuth2 authorization code
     *
     * @param code Authorization code from Google OAuth2 flow
     * @return AuthenticationResponse with JWT token
     */
    AuthenticationResponse authenticateWithGoogle(String code);

    /**
     * Setup password for Google users
     *
     * @param email       User email
     * @param newPassword New password
     */
    void setupPasswordForGoogleUser(String email, String newPassword);

    /**
     * Get password setup status for a user
     *
     * @param email User email
     * @return true if password setup is required, false otherwise
     */
    boolean getPasswordSetupStatus(String email);

    /**
     * Change password for a user
     *
     * @param email           User email
     * @param currentPassword Current password
     * @param newPassword     New password
     */
    void changePassword(String email, String currentPassword, String newPassword);
}
