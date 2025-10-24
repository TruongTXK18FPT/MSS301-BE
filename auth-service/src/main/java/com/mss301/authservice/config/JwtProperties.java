package com.mss301.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String signerKey;

    public String getSignerKey() {
        return signerKey;
    }

    public void setSignerKey(String signerKey) {
        this.signerKey = signerKey;
    }
}
