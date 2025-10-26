package com.mss301.notificationservice.controller;

import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.event.NotificationEvent;
import com.mss301.notificationservice.event.PasswordResetOtpEvent;
import com.mss301.notificationservice.service.EmailService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    EmailService emailService;

    @Bean
    public Consumer<NotificationEvent> notificationDelivery() {
        return event -> {
            log.info("Received notification event: {}", event);
            emailService.sendEmail(EmailRequest.builder()
                    .to(event.getRecipient())
                    .subject(event.getSubject())
                    .templateName(event.getTemplateCode())
                    .templateData(event.getParam())
                    .build());
        };
    }

    @Bean
    public Consumer<Map<String, Object>> guardianVerificationDelivery() {
        return eventMap -> {
            log.info("Received guardian verification event: {}", eventMap);

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("studentName", eventMap.get("studentName"));
            templateData.put("guardianName", eventMap.get("guardianName"));
            templateData.put("verificationCode", eventMap.get("verificationCode"));
            templateData.put("relationship", eventMap.get("relationship"));
            templateData.put("expiryMinutes", eventMap.get("expiryMinutes"));

            emailService.sendEmail(EmailRequest.builder()
                    .to((String) eventMap.get("studentEmail"))
                    .subject((String) eventMap.get("subject"))
                    .templateName("guardian_verification_otp")
                    .templateData(templateData)
                    .build());
        };
    }

    @Bean
    public Consumer<PasswordResetOtpEvent> passwordResetOtpDelivery() {
        return event -> {
            log.info("Received password reset OTP event: {}", event);

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("otpCode", event.getOtpCode());
            templateData.put("expiryMinutes", event.getExpiryMinutes());

            emailService.sendEmail(EmailRequest.builder()
                    .to(event.getUserEmail())
                    .subject(event.getSubject())
                    .templateName("password_reset_otp")
                    .templateData(templateData)
                    .build());
        };
    }
}
