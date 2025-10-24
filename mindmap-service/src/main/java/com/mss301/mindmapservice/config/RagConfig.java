package com.mss301.mindmapservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RagConfig {

    @Value("${rag.rag-service.url}")
    private String ragServiceUrl;

    @Bean
    public WebClient ragWebClient() {
        return WebClient.builder().baseUrl(ragServiceUrl).build();
    }
}
