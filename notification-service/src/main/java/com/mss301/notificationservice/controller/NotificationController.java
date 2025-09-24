package com.mss301.notificationservice.controller;

import org.springframework.web.bind.annotation.*;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.dto.response.ApiResponse;
import com.mss301.notificationservice.dto.response.EmailResponse;
import com.mss301.notificationservice.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/email")
    public ApiResponse<EmailResponse> sendEmail(@RequestBody EmailRequest request) {
        log.info("Sending email to: {}", request.getTo());
        EmailResponse response = emailService.sendEmail(request);
        return ApiResponse.success("Email sent", response);
    }

    @PostMapping("/email/template")
    public ApiResponse<EmailResponse> sendEmailWithTemplate(@RequestBody EmailRequest request) {
        log.info("Sending template email to: {}", request.getTo());
        EmailResponse response = emailService.sendEmailWithTemplate(request);
        return ApiResponse.success("Template email sent", response);
    }

    @PostMapping("/email/verification")
    public ApiResponse<EmailResponse> sendVerificationEmail(@RequestParam String email, @RequestParam String token) {
        log.info("Sending verification email to: {}", email);
        EmailResponse response = emailService.sendVerificationEmail(email, token);
        return ApiResponse.success("Verification email sent", response);
    }

    @PostMapping("/email/password-reset")
    public ApiResponse<EmailResponse> sendPasswordResetEmail(@RequestParam String email, @RequestParam String token) {
        log.info("Sending password reset email to: {}", email);
        EmailResponse response = emailService.sendPasswordResetEmail(email, token);
        return ApiResponse.success("Password reset email sent", response);
    }
}
