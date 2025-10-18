package com.mss301.documentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

public class EmbeddingFeignConfig {

    @Value("${embedding.client.api-key:}")
    private String apiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + apiKey);
            template.header("Content-Type", "application/json; charset=utf-8");
        };
    }
}
