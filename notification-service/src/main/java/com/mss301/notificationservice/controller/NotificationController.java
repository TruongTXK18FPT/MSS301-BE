package com.mss301.notificationservice.controller;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mss301.notificationservice.dto.request.EmailRequest;
import com.mss301.notificationservice.event.NotificationEvent;
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
}
