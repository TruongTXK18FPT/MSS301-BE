package com.mss301.paymentservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment.payos")
@Data
@Slf4j
public class PayOSConfig {
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String baseUrl;
    private String webhookUrl;
    private String returnUrl;
    private String cancelUrl;

    @PostConstruct
    public void logConfiguration() {
        log.info("=== PayOS Configuration ===");
        log.info("Base URL: {}", baseUrl);
        log.info("Return URL: {}", returnUrl);
        log.info("Cancel URL: {}", cancelUrl);
        log.info("Webhook URL: {}", webhookUrl);
        log.info("===========================");
    }
}
