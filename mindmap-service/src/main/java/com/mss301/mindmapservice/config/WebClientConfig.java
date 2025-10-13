package com.mss301.mindmapservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }

    @Bean
    public WebClient mistralWebClient() {
        return webClientBuilder().baseUrl("https://api.mistral.ai/v1").build();
    }

    @Bean
    public WebClient geminiWebClient() {
        return webClientBuilder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }
}
