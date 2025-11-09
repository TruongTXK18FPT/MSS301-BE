package com.mss301.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mss301.authservice.entity.OTP;

public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByEmailAndOtpAndUsedFalseAndPurpose(String email, String otp, OTP.OtpPurpose purpose);

    List<OTP> findByEmailAndUsedFalse(String email);

    List<OTP> findByEmailAndPurposeAndUsedFalse(String email, OTP.OtpPurpose purpose);

    // Alternative method with explicit query to debug the issue
    // Check expiry time in query to ensure we only get valid, non-expired OTPs
    @Query("SELECT o FROM OTP o WHERE o.email = :email AND o.otp = :otp AND o.used = false AND o.purpose = :purpose AND o.expiryTime > :currentTime")
    Optional<OTP> findValidOTP(
            @Param("email") String email,
            @Param("otp") String otp,
            @Param("purpose") OTP.OtpPurpose purpose,
            @Param("currentTime") java.time.LocalDateTime currentTime);

    // Debug method to check all OTPs for an email
    @Query("SELECT o FROM OTP o WHERE o.email = :email ORDER BY o.createdAt DESC")
    List<OTP> findAllByEmail(@Param("email") String email);

    // Get current valid OTP for email verification
    @Query("SELECT o FROM OTP o WHERE o.email = :email AND o.used = false AND o.purpose = :purpose AND o.expiryTime > :currentTime ORDER BY o.createdAt DESC")
    Optional<OTP> findCurrentValidOTP(
            @Param("email") String email,
            @Param("purpose") OTP.OtpPurpose purpose,
            @Param("currentTime") java.time.LocalDateTime currentTime);
}
