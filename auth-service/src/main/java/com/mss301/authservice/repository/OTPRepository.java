package com.mss301.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.authservice.entity.OTP;

public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByEmailAndOtpAndUsedFalseAndPurpose(String email, String otp, OTP.OtpPurpose purpose);

    List<OTP> findByEmailAndUsedFalse(String email);

    List<OTP> findByEmailAndPurposeAndUsedFalse(String email, OTP.OtpPurpose purpose);
}
