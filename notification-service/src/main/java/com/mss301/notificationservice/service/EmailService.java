package com.mss301.notificationservice.service;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.dto.response.EmailResponse;

public interface EmailService {
    EmailResponse sendEmail(EmailRequest request);

    EmailResponse sendEmailWithTemplate(EmailRequest request);

    EmailResponse sendVerificationEmail(String toEmail, String verificationToken);

    EmailResponse sendPasswordResetEmail(String toEmail, String resetToken);

    EmailResponse sendWelcomeEmail(String toEmail, String userName);
}
