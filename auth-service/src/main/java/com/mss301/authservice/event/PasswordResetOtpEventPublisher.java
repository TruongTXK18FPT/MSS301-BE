package com.mss301.authservice.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.mss301.authservice.event.PasswordResetOtpEvent;

@Component
@Slf4j
public class PasswordResetOtpEventPublisher {

    @Autowired
    private StreamBridge streamBridge;

    public void publishPasswordResetOtpEvent(String userEmail, String otpCode, Integer expiryMinutes) {
        try {
            PasswordResetOtpEvent event = PasswordResetOtpEvent.builder()
                    .userEmail(userEmail)
                    .otpCode(otpCode)
                    .expiryMinutes(expiryMinutes)
                    .subject("Mã OTP Đặt Lại Mật Khẩu - MathMind")
                    .build();

            streamBridge.send("passwordResetOtpDelivery-out-0", event);
            log.info("Password reset OTP event published for user: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to publish password reset OTP event for user: {}", userEmail, e);
            throw new RuntimeException("Failed to publish password reset OTP event: " + e.getMessage());
        }
    }
}
