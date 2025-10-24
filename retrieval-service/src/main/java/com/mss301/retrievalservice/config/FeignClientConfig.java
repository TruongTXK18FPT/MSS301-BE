package com.mss301.retrievalservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Value("${embedding.client.api-key:}")
    private String apiKey;

    @Bean
    public RequestInterceptor embeddingRequestInterceptor() {
        return template -> {
            // Only add header if this is for embedding service
            if (template.url().contains("/embeddings")) {
                log.info("🔑 Full API Key length: {}, Value: [{}]", apiKey != null ? apiKey.length() : 0, apiKey);
                log.info("🔑 Authorization header will be: [Bearer {}]", apiKey);
                template.header("Authorization", "Bearer " + apiKey);
            }
        };
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
