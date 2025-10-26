package com.mss301.authservice.service.impl;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.config.EventPublisher;
import com.mss301.authservice.event.PasswordResetOtpEventPublisher;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;
import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.entity.*;
import com.mss301.authservice.event.CreatedUserEvent;
import com.mss301.authservice.event.NotificationEvent;
import com.mss301.authservice.repository.*;
import com.mss301.authservice.service.AuthenticationService;
import com.mss301.authservice.service.GoogleOAuthService;
import com.mss301.authservice.service.GoogleUserService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final OTPRepository otpRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleOAuthService googleOAuthService;
    private final GoogleUserService googleUserService;
    private final EventPublisher eventPublisher;
    private final PasswordResetOtpEventPublisher passwordResetOtpEventPublisher;

    @Value("${jwt.signerKey:mySecretKey}")
    private String signerKey;

    @Value("${jwt.valid-duration:3600}")
    private long validDuration;

    @Value("${jwt.refreshable-duration:86400}")
    private long refreshableDuration;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String googleRedirectUri;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new RuntimeException("Unauthenticated");
        }

        if (user.getStatus() != UserAccount.UserStatus.ACTIVE) {
            throw new RuntimeException("User is not active");
        }

        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email chưa được xác thực. Vui lòng kiểm tra email và nhập mã OTP để xác thực.");
        }

        var token = generateToken(user);

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token(token)
                .expiryTime(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                .build();
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;
        String userId = null;
        String email = null;

        try {
            verifyToken(token, false);

            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            userId = claims.getSubject();
            email = claims.getStringClaim("email");

        } catch (Exception e) {
            isValid = false;
        }

        String role = null;
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            role = claims.getStringClaim("role");
            log.info("Introspect - User: {}, Role from token: {}", email, role);
        } catch (Exception e) {
            log.error("Error parsing role from token: {}", e.getMessage());
        }

        log.info("Introspect response for user {}: valid={}, id={}, email={}, role={}",
                email, isValid, userId, email, role);

        return IntrospectResponse.builder()
                .valid(isValid)
                .id(userId)
                .email(email)
                .role(role)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime())
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (Exception e) {
            log.info("Token already expired or invalid");
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) {
        try {
            var signedJWT = verifyToken(request.getToken(), true);
            String userId = signedJWT.getJWTClaimsSet().getSubject();

            var user = userRepository
                    .findById(Long.parseLong(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            var token = generateToken(user);

            return AuthenticationResponse.builder()
                    .token(token)
                    .expiryTime(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Verifying email: {} with OTP: {}", request.getEmail(), request.getOtpCode());

        // Debug: Check all OTPs for this email
        var allOtps = otpRepository.findAllByEmail(request.getEmail());
        log.info(
                "All OTPs for email {}: {}",
                request.getEmail(),
                allOtps.stream()
                        .map(o -> String.format(
                                "OTP=%s, Used=%s, Purpose=%s, Expiry=%s",
                                o.getOtp(), o.isUsed(), o.getPurpose(), o.getExpiryTime()))
                        .toList());

        // Use explicit query method to avoid Spring Data JPA naming issues
        var otp = otpRepository
                .findValidOTP(request.getEmail(), request.getOtpCode(), OTP.OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> {
                    log.error("Invalid OTP for email: {} with OTP: {}", request.getEmail(), request.getOtpCode());
                    return new RuntimeException("Invalid OTP");
                });

        log.info("Found OTP: {} for email: {}, expiry: {}", otp.getOtp(), otp.getEmail(), otp.getExpiryTime());

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.error(
                    "OTP expired for email: {}, expiry: {}, current: {}",
                    request.getEmail(),
                    otp.getExpiryTime(),
                    LocalDateTime.now());
            throw new RuntimeException("OTP expired");
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);
        log.info("Marked OTP as used for email: {}", request.getEmail());

        // Update user email verification status
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Updated email verification status for user: {}", request.getEmail());

        // Send welcome email after successful verification
        Map<String, Object> welcomeData = new HashMap<>();
        welcomeData.put("fullName", "User"); // Use generic name for personalization

        NotificationEvent welcomeEvent = NotificationEvent.builder()
                .recipient(user.getEmail())
                .subject("Welcome to MSS301!")
                .templateCode("welcome_email") // Now use welcome template
                .param(welcomeData)
                .build();

        eventPublisher.publishNotificationEvent(welcomeEvent);
        log.info("Sent welcome email to verified user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for email: {} with OTP: {}", request.getEmail(), request.getOtpCode());

        var otp = otpRepository
                .findValidOTP(request.getEmail(), request.getOtpCode(), OTP.OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> {
                    log.error(
                            "Invalid OTP for password reset for email: {} with OTP: {}",
                            request.getEmail(),
                            request.getOtpCode());
                    return new RuntimeException("Invalid OTP");
                });

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.error(
                    "OTP expired for password reset for email: {}, expiry: {}, current: {}",
                    request.getEmail(),
                    otp.getExpiryTime(),
                    LocalDateTime.now());
            throw new RuntimeException("OTP expired");
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);
        log.info("Marked password reset OTP as used for email: {}", request.getEmail());

        // Update user password
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Updated password for user: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void setupPasswordForGoogleUser(String email, String newPassword) {
        // Reuse existing logic from resetPassword but without OTP validation
        var user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is Google user
        if (!user.getIsGoogleUser()) {
            throw new RuntimeException("User is not a Google user");
        }

        // Check if password setup is required
        if (!user.getPasswordSetupRequired()) {
            throw new RuntimeException("Password already set for this user");
        }

        // Update password (reuse existing logic)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSetupRequired(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void createPassword(String userId, PasswordCreationRequest request) {
        var user = userRepository
                .findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void sendEmailVerification(String email) {
        // Generate OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Invalidate previous OTPs
        var previousOtps = otpRepository.findByEmailAndPurposeAndUsedFalse(email, OTP.OtpPurpose.EMAIL_VERIFICATION);
        previousOtps.forEach(otp -> otp.setUsed(true));
        otpRepository.saveAll(previousOtps);

        // Create new OTP
        OTP otp = OTP.builder()
                .email(email)
                .otp(otpCode)
                .purpose(OTP.OtpPurpose.EMAIL_VERIFICATION)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(otp);

        // Send email notification via EventPublisher
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("OTP", otpCode); // Capital letters to match template variable
        templateData.put("PURPOSE", "EMAIL_VERIFICATION"); // Add PURPOSE for template

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .recipient(email)
                .subject("Mã xác thực Email - MSS301")
                .templateCode("otp_verified") // Use OTP template not welcome template
                .param(templateData)
                .build();

        eventPublisher.publishNotificationEvent(notificationEvent);
        log.info("Sent email verification OTP to: {}", email);
    }

    @Override
    public void sendPasswordResetOTP(String email) {
        // Check if user exists
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        // Generate OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Invalidate previous OTPs
        var previousOtps = otpRepository.findByEmailAndPurposeAndUsedFalse(email, OTP.OtpPurpose.PASSWORD_RESET);
        previousOtps.forEach(otp -> otp.setUsed(true));
        otpRepository.saveAll(previousOtps);

        // Create new OTP
        OTP otp = OTP.builder()
                .email(email)
                .otp(otpCode)
                .purpose(OTP.OtpPurpose.PASSWORD_RESET)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(otp);

        // Send password reset OTP via dedicated event
        try {
            passwordResetOtpEventPublisher.publishPasswordResetOtpEvent(
                    email,
                    otpCode,
                    5);
            log.info("Password reset OTP event published for: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP event for email: {}", email, e);
            // Don't fail the operation if event publishing fails
        }
    }

    @Override
    public void resendOTP(String email) {
        // Check if user exists
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Invalidate previous OTPs for email verification
        var previousOtps = otpRepository.findByEmailAndPurposeAndUsedFalse(email, OTP.OtpPurpose.EMAIL_VERIFICATION);
        previousOtps.forEach(otp -> otp.setUsed(true));
        otpRepository.saveAll(previousOtps);

        // Create new OTP
        OTP otp = OTP.builder()
                .email(email)
                .otp(otpCode)
                .purpose(OTP.OtpPurpose.EMAIL_VERIFICATION)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(otp);

        // Send email notification via EventPublisher
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("OTP", otpCode); // Capital letters
        templateData.put("PURPOSE", "EMAIL_VERIFICATION"); // Add PURPOSE

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .recipient(email)
                .subject("Mã xác thực Email - MSS301")
                .templateCode("otp_verified") // Use OTP template
                .param(templateData)
                .build();

        eventPublisher.publishNotificationEvent(notificationEvent);
        log.info("Resent OTP to email: {}", email);
    }

    private String generateToken(UserAccount user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        String roleName = null;
        if (user.getRoleId() != null) {
            try {
                Optional<Role> roleOpt = Optional.ofNullable(user.getRole());
                if (roleOpt.isPresent()) {
                    roleName = roleOpt.get().getName();
                    log.info("User {} has role: {}", user.getEmail(), roleName);
                } else {
                    log.warn("User {} has roleId {} but role is null", user.getEmail(), user.getRoleId());
                }
            } catch (Exception e) {
                log.error("Error getting role for user {}: {}", user.getEmail(), e.getMessage());
            }
        } else {
            log.warn("User {} has no roleId", user.getEmail());
        }

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("mss301.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("role", roleName)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                        .plus(refreshableDuration, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) {
            throw new RuntimeException("Token invalid");
        }

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new RuntimeException("Token invalid");
        }

        return signedJWT;
    }

    @Override
    @Transactional
    public AuthenticationResponse authenticateWithGoogle(String code) {
        try {
            log.info("Starting Google OAuth authentication with code: {}", code);

            // Get Google user info
            GoogleUserInfoResponse userInfo = getGoogleUserInfo(code);
            log.info("Retrieved Google user info for email: {}", userInfo.getEmail());

            // Handle user authentication
            UserAccount user = handleGoogleUser(userInfo);

            // Generate JWT token
            String jwtToken = generateToken(user);

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .expiryTime(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                    .authenticated(true)
                    .email(user.getEmail())
                    .name("User")
                    .build();

        } catch (Exception e) {
            log.error("Google OAuth authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Google OAuth authentication failed: " + e.getMessage());
        }
    }

    /**
     * Get Google user information from authorization code
     */
    private GoogleUserInfoResponse getGoogleUserInfo(String code) {
        GoogleOAuthTokenResponse tokenResponse = googleOAuthService.exchangeToken(code);
        return googleOAuthService.getUserInfo(tokenResponse.getAccessToken());
    }

    /**
     * Handle Google user - create new or authenticate existing
     */
    private UserAccount handleGoogleUser(GoogleUserInfoResponse userInfo) {
        if (!googleUserService.userExists(userInfo.getEmail())) {
            return createNewGoogleUser(userInfo);
        } else {
            return authenticateExistingGoogleUser(userInfo);
        }
    }

    /**
     * Create new Google user
     */
    private UserAccount createNewGoogleUser(GoogleUserInfoResponse userInfo) {
        log.info("Creating new Google user for email: {}", userInfo.getEmail());
        UserAccount newUser = googleUserService.createGoogleUser(userInfo, "STUDENT"); // Default to STUDENT for Google
        // login

        // Publish user created event
        publishUserCreatedEvent(newUser, userInfo);

        return newUser;
    }

    /**
     * Authenticate existing Google user
     */
    private UserAccount authenticateExistingGoogleUser(GoogleUserInfoResponse userInfo) {
        UserAccount user = googleUserService.getUserByEmail(userInfo.getEmail());
        log.info("Existing user logged in with Google: {}", user.getEmail());

        // Check if user role is allowed for Google login (only STUDENT)
        // GUARDIAN and TEACHER must use regular login, not Google OAuth
        if (!user.getRole().getName().equals("STUDENT")) {
            log.warn("Google login not allowed for role: {} for user: {}", user.getRole().getName(), user.getEmail());
            throw new RuntimeException(
                    "Google login is only available for students. Users with " + user.getRole().getName() +
                            " role must use regular login with email and password.");
        }

        googleUserService.updateLastLogin(user);
        log.info("Google OAuth authentication successful for user: {}", user.getEmail());

        return user;
    }

    /**
     * Publish user created event for Google users
     *
     * @param user     Created user
     * @param userInfo Google user info
     */
    private void publishUserCreatedEvent(UserAccount user, GoogleUserInfoResponse userInfo) {
        try {
            // Create CreatedUserEvent with Google user details
            CreatedUserEvent event = CreatedUserEvent.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .fullName(userInfo.getName()) // Set fullName from Google user info
                    .userType("STUDENT") // Default for Google users
                    .build();

            eventPublisher.publishCreatedUserEvent(event);
            log.info("Published CreatedUserEvent for Google user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish CreatedUserEvent for Google user: {}", user.getEmail(), e);
        }
    }

    @Override
    public boolean getPasswordSetupStatus(String email) {
        log.info("Checking password setup status for email: {}", email);

        try {
            UserAccount user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Check if user has a password set (not null and not empty)
            boolean hasPassword = user.getPassword() != null && !user.getPassword().trim().isEmpty();

            log.info("Password setup status for {}: {}", email, !hasPassword);
            return !hasPassword; // Return true if password setup is required

        } catch (Exception e) {
            log.error("Failed to check password setup status for email: {}: {}", email, e.getMessage(), e);
            return false; // Default to not requiring password setup on error
        }
    }

    @Override
    public void changePassword(String email, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", email);
        log.info("Current password provided: {}", currentPassword);
        log.info("New password provided: {}", newPassword);

        try {
            // Find user by email
            UserAccount user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            log.info("User found: {}", user.getEmail());
            log.info("User has password: {}", user.getPassword() != null);
            if (user.getPassword() != null) {
                log.info("Stored password hash: {}",
                        user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "...");
            }

            // Check if user has a password set (for Google users who haven't setup password
            // yet)
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                log.warn("User {} has no password set. Please setup password first.", email);
                throw new RuntimeException(
                        "No password set. Please setup your password first using the setup password feature.");
            }

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                log.warn("Invalid current password for user: {}", email);
                throw new RuntimeException("Current password is incorrect");
            }

            // Validate new password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new RuntimeException("New password cannot be empty");
            }

            if (newPassword.length() < 6) {
                throw new RuntimeException("New password must be at least 6 characters long");
            }

            // Encode and set new password
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedNewPassword);

            // Save user
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", email);

        } catch (Exception e) {
            log.error("Failed to change password for user: {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }
}
