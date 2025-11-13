package com.mss301.documentservice.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleFileSearchClientConfig {

    @Bean
    public ErrorDecoder googleFileSearchErrorDecoder() {
        return new GoogleFileSearchErrorDecoder();
    }

    public static class GoogleFileSearchErrorDecoder implements ErrorDecoder {
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            return new RuntimeException("Google File Search API error: " + response.status() + " - " + response.reason());
        }
    }
}

