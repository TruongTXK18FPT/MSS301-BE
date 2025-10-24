package com.mss301.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for frontend URLs
 * Follows Spring Boot best practices for externalized configuration
 */
@Component
@ConfigurationProperties(prefix = "frontend")
@Data
public class FrontendProperties {

    private String baseUrl;
    private String dashboardUrl;
    private String passwordSetupUrl;
    private String loginUrl;
}
