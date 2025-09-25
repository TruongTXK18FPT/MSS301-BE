package com.mss301.authservice.dto.response;

import java.time.LocalDateTime;

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
public class ExchangeTokenResponse {

    String accessToken;

    String refreshToken;

    String tokenType;

    @Builder.Default
    Long expiresIn = 3600L; // 1 hour in seconds

    String scope;

    LocalDateTime issuedAt;

    LocalDateTime expiresAt;

    String userId;

    String email;
}
