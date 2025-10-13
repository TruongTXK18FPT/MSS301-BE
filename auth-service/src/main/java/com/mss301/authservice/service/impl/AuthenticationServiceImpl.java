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

import com.mss301.authservice.client.GoogleOAuthClient;
import com.mss301.authservice.client.GoogleUserInfoClient;
import com.mss301.authservice.config.EventPublisher;
import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;
import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.entity.*;
import com.mss301.authservice.event.NotificationEvent;
import com.mss301.authservice.repository.*;
import com.mss301.authservice.service.AuthenticationService;
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
    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleUserInfoClient googleUserInfoClient;
    private final EventPublisher eventPublisher;

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
        String username = null;

        try {
            verifyToken(token, false);

            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            userId = claims.getSubject();
            email = claims.getStringClaim("email");
            username = claims.getStringClaim("username");

        } catch (Exception e) {
            isValid = false;
        }

        String role = null;
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            role = claims.getStringClaim("role");
        } catch (Exception ignored) {
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .id(userId)
                .email(email)
                .username(username)
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
        var otp = otpRepository
                .findByEmailAndOtpAndUsedFalseAndPurpose(
                        request.getEmail(), request.getOtpCode(), OTP.OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        // Update user email verification status
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        // Send welcome email after successful verification
        Map<String, Object> welcomeData = new HashMap<>();
        welcomeData.put("fullName", user.getUsername()); // Or use actual fullName if available

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
        var otp = otpRepository
                .findByEmailAndOtpAndUsedFalseAndPurpose(
                        request.getEmail(), request.getOtpCode(), OTP.OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        // Update user password
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
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

        // Send email notification via EventPublisher
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("OTP", otpCode); // Capital letters
        templateData.put("PURPOSE", "PASSWORD_RESET"); // Add PURPOSE

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .recipient(email)
                .subject("Mã đặt lại mật khẩu - MSS301")
                .templateCode("otp_verified") // Use OTP template
                .param(templateData)
                .build();

        eventPublisher.publishNotificationEvent(notificationEvent);
        log.info("Sent password reset OTP to: {}", email);
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
                }
            } catch (Exception ignored) {
            }
        }

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("mss301.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
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

            // Step 1: Exchange authorization code for access token
            GoogleOAuthTokenResponse tokenResponse = googleOAuthClient.exchangeToken(
                    code, googleClientId, googleClientSecret, googleRedirectUri, "authorization_code");

            if (tokenResponse.getAccessToken() == null) {
                throw new RuntimeException("Failed to exchange Google authorization code for access token");
            }

            // Step 2: Get user information from Google
            GoogleUserInfoResponse userInfo = googleUserInfoClient.getUserInfo("json", tokenResponse.getAccessToken());

            if (userInfo.getEmail() == null) {
                throw new RuntimeException("Failed to retrieve user information from Google");
            }

            log.info("Retrieved Google user info for email: {}", userInfo.getEmail());

            // Step 3: Check if user exists in our database
            Optional<UserAccount> existingUser = userRepository.findByEmail(userInfo.getEmail());

            if (existingUser.isEmpty()) {
                // Do NOT auto-create. Ask FE to proceed to registration with prefilled Google
                // data
                log.info(
                        "Google email not found in DB. Returning REGISTRATION_REQUIRED for email: {}",
                        userInfo.getEmail());

                return AuthenticationResponse.builder()
                        .authenticated(false)
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .givenName(userInfo.getGivenName())
                        .familyName(userInfo.getFamilyName())
                        .picture(userInfo.getPicture())
                        .build();
            }

            // Existing user: issue JWT
            UserAccount user = existingUser.get();
            log.info("Existing user logged in with Google: {}", user.getEmail());

            String jwtToken = generateToken(user);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Google OAuth authentication successful for user: {}", user.getEmail());

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .expiryTime(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                    .authenticated(true)
                    .build();

        } catch (Exception e) {
            log.error("Google OAuth authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Google OAuth authentication failed: " + e.getMessage());
        }
    }
}
