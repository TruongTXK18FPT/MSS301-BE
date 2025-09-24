package com.mss301.notificationservice.service.impl;

import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.dto.response.EmailResponse;
import com.mss301.notificationservice.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from:noreply@mss301.com}")
    private String fromEmail;

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        try {
            log.info("Sending email to: {}", request.getTo());

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());

            String content;
            if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
                // Use template
                Context context = new Context();
                if (request.getTemplateData() != null) {
                    if (request.getTemplateData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> templateVars = (Map<String, Object>) request.getTemplateData();
                        templateVars.forEach(context::setVariable);
                    }
                }
                content = templateEngine.process(request.getTemplateName(), context);
            } else {
                // Use direct content
                content = request.getContent();
            }

            helper.setText(content, true); // true = HTML content

            mailSender.send(mimeMessage);

            log.info("Email sent successfully to: {}", request.getTo());

            return EmailResponse.builder()
                    .messageId("EMAIL-" + System.currentTimeMillis())
                    .status("SENT")
                    .message("Email sent successfully")
                    .build();

        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            return EmailResponse.builder()
                    .status("FAILED")
                    .message("Failed to send email: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendEmailWithTemplate(EmailRequest request) {
        return sendEmail(request); // Template logic is already in sendEmail method
    }

    @Override
    public EmailResponse sendVerificationEmail(String toEmail, String verificationToken) {
        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject("Email Verification - MSS301")
                .templateName("otp_verified")
                .templateData(Map.of("PURPOSE", "EMAIL_VERIFICATION", "OTP", verificationToken))
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendPasswordResetEmail(String toEmail, String resetToken) {
        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject("Password Reset - MSS301")
                .templateName("otp_verified")
                .templateData(Map.of("PURPOSE", "PASSWORD_RESET", "OTP", resetToken))
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendWelcomeEmail(String toEmail, String userName) {
        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject("Welcome to MSS301!")
                .templateName("welcome_email")
                .templateData(Map.of("userName", userName))
                .build();

        return sendEmail(request);
    }

    // Kafka Listeners for handling events from other services
    @KafkaListener(topics = "email-verification")
    public void handleEmailVerificationEvent(String message) {
        log.info("Received email verification event: {}", message);
        try {
            // Parse the Kafka message and extract email and OTP
            // This is a simple implementation - you might want to use JSON parsing
            String[] parts = message.split(",");
            if (parts.length >= 2) {
                String email = parts[0];
                String otp = parts[1];
                sendVerificationEmail(email, otp);
            }
        } catch (Exception e) {
            log.error("Failed to process email verification event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "password-reset")
    public void handlePasswordResetEvent(String message) {
        log.info("Received password reset event: {}", message);
        try {
            String[] parts = message.split(",");
            if (parts.length >= 2) {
                String email = parts[0];
                String otp = parts[1];
                sendPasswordResetEmail(email, otp);
            }
        } catch (Exception e) {
            log.error("Failed to process password reset event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "user-created")
    public void handleUserCreatedEvent(String message) {
        log.info("Received user created event: {}", message);
        try {
            String[] parts = message.split(",");
            if (parts.length >= 2) {
                String email = parts[0];
                String userName = parts.length > 2 ? parts[2] : email;
                sendWelcomeEmail(email, userName);
            }
        } catch (Exception e) {
            log.error("Failed to process user created event: {}", e.getMessage());
        }
    }
}
