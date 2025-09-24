package com.mss301.authservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    String otp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OtpPurpose purpose;

    @Column(nullable = false)
    @Builder.Default
    boolean used = false;

    @Column(nullable = false)
    LocalDateTime expiryTime;

    @Column(nullable = false)
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    public enum OtpPurpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
