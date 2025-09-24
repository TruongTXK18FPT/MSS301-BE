package com.mss301.authservice.service.impl;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.dto.request.*;
import com.mss301.authservice.dto.response.AuthenticationResponse;
import com.mss301.authservice.dto.response.IntrospectResponse;
import com.mss301.authservice.entity.*;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.signerKey:mySecretKey}")
    private String signerKey;

    @Value("${jwt.valid-duration:3600}")
    private long validDuration;

    @Value("${jwt.refreshable-duration:86400}")
    private long refreshableDuration;

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

        return IntrospectResponse.builder()
                .valid(isValid)
                .id(userId)
                .email(email)
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
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        otpRepository.save(otp);

        // Send email notification via Kafka
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("email", email);
        emailData.put("otp", otpCode);
        emailData.put("purpose", "EMAIL_VERIFICATION");

        kafkaTemplate.send("email-verification", emailData);
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
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        otpRepository.save(otp);

        // Send email notification via Kafka
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("email", email);
        emailData.put("otp", otpCode);
        emailData.put("purpose", "PASSWORD_RESET");

        kafkaTemplate.send("password-reset", emailData);
    }

    private String generateToken(UserAccount user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("mss301.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("tenant", user.getTenant() != null ? user.getTenant().getId() : null)
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
}
