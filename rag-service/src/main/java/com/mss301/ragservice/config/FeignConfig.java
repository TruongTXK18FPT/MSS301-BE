package com.mss301.ragservice.config;

import com.mss301.ragservice.exception.RagServiceException;
import feign.Logger;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            String errorMessage = String.format(
                    "Error calling Retrieval Service [%s]: status=%d",
                    methodKey,
                    response.status()
            );

            log.error(errorMessage);

            if (response.status() >= 400 && response.status() < 500) {
                return new RagServiceException(
                        "Client error from Retrieval Service: " + response.status()
                );
            }

            if (response.status() >= 500) {
                return new RagServiceException(
                        "Server error from Retrieval Service: " + response.status()
                );
            }

            return new RagServiceException("Unknown error: " + response.status());
        };
    }
}
