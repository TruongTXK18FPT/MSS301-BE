package com.mss301.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExchangeTokenRequest {

    @NotBlank(message = "Authorization code is required")
    String code;

    @NotBlank(message = "Client ID is required")
    String clientId;

    @NotBlank(message = "Client secret is required")
    String clientSecret;

    @NotBlank(message = "Redirect URI is required")
    String redirectUri;

    @Builder.Default
    String grantType = "authorization_code";
}
