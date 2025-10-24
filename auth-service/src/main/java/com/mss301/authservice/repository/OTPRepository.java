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
    @Query("SELECT o FROM OTP o WHERE o.email = :email AND o.otp = :otp AND o.used = false AND o.purpose = :purpose")
    Optional<OTP> findValidOTP(
            @Param("email") String email, @Param("otp") String otp, @Param("purpose") OTP.OtpPurpose purpose);

    // Debug method to check all OTPs for an email
    @Query("SELECT o FROM OTP o WHERE o.email = :email ORDER BY o.createdAt DESC")
    List<OTP> findAllByEmail(@Param("email") String email);
}
