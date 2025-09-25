package com.mss301.notificationservice.service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.exception.AppException;
import com.mss301.notificationservice.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendEmail(EmailRequest email) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            Context context = new Context();
            if (email.getTemplateData() != null) {
                context.setVariables(email.getTemplateData());
            }

            String htmlContent = templateEngine.process(email.getTemplateName(), context);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }
}
