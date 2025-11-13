package com.mss301.chatbotservice.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DotenvConfig {

    @PostConstruct
    public void loadEnv() {
        // Try to load .env from multiple locations
        Dotenv dotenv = null;
        
        // Try chatbot-service directory first
        try {
            dotenv = Dotenv.configure()
                    .directory("./chatbot-service")
                    .ignoreIfMissing()
                    .load();
            if (dotenv != null && !dotenv.entries().isEmpty()) {
                log.info("Loaded .env from ./chatbot-service directory");
            }
        } catch (Exception e) {
            log.debug("Could not load .env from ./chatbot-service: {}", e.getMessage());
        }
        
        // If not found, try root directory
        if (dotenv == null || dotenv.entries().isEmpty()) {
            try {
                dotenv = Dotenv.configure()
                        .directory(".")
                        .ignoreIfMissing()
                        .load();
                if (dotenv != null && !dotenv.entries().isEmpty()) {
                    log.info("Loaded .env from root directory");
                }
            } catch (Exception e) {
                log.debug("Could not load .env from root directory: {}", e.getMessage());
            }
        }
        
        // If still not found, try parent directory (MSS301-BE)
        if (dotenv == null || dotenv.entries().isEmpty()) {
            try {
                dotenv = Dotenv.configure()
                        .directory("..")
                        .ignoreIfMissing()
                        .load();
                if (dotenv != null && !dotenv.entries().isEmpty()) {
                    log.info("Loaded .env from parent directory");
                }
            } catch (Exception e) {
                log.debug("Could not load .env from parent directory: {}", e.getMessage());
            }
        }

        // Set system properties from .env file
        if (dotenv != null && !dotenv.entries().isEmpty()) {
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                // Don't log sensitive values, but log that they were set
                if (key.contains("API_KEY") || key.contains("SECRET")) {
                    log.info("Set system property: {} (length: {})", key, value != null ? value.length() : 0);
                } else {
                    log.debug("Set system property: {} = {}", key, value);
                }
                System.setProperty(key, value);
            });
        } else {
            log.warn("No .env file found. Please set environment variables GEMINI_API_KEY and MISTRAL_API_KEY");
        }
    }
}