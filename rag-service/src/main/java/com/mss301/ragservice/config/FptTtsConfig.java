package com.mss301.ragservice.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;

import feign.Logger;
import feign.codec.Encoder;

@Configuration
public class FptTtsConfig {
    @Bean
    public Encoder feignEncoder() {
        return new SpringEncoder(
                () -> new HttpMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8)));
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
