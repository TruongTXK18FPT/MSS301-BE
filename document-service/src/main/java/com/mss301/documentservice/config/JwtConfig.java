package com.mss301.documentservice.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Bean
    public JwtDecoder jwtDecoder() {
        // For HMAC-based JWT (symmetric key) - chỉ cần decoder, không cần encoder
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(signerKey.getBytes(), "HS512"))
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}

