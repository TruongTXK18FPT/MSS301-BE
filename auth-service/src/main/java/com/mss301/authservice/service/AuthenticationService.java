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
}
