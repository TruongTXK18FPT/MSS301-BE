package com.mss301.authservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth token exchange
 * Maps to Google's OAuth2 token endpoint parameters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleOAuthTokenRequest {
    private String code;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String grantType;
}
