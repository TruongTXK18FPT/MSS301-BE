package com.mss301.authservice.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mss301.authservice.client.GoogleOAuthClient;
import com.mss301.authservice.client.GoogleUserInfoClient;
import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;
import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.service.GoogleOAuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Google OAuth Service
 * Follows Single Responsibility Principle (SRP) and Dependency Inversion
 * Principle (DIP)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleUserInfoClient googleUserInfoClient;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String googleRedirectUri;

    @Override
    public GoogleOAuthTokenResponse exchangeToken(String code) {
        try {
            log.info("Exchanging authorization code for access token");

            // Create form data for OAuth token exchange
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("code", code);
            formData.add("client_id", googleClientId);
            formData.add("client_secret", googleClientSecret);
            formData.add("redirect_uri", googleRedirectUri);
            formData.add("grant_type", "authorization_code");

            GoogleOAuthTokenResponse tokenResponse = googleOAuthClient.exchangeToken(formData);

            log.info("Token response received: {}", tokenResponse);
            log.info("Access token: {}", tokenResponse.getAccessToken());

            if (tokenResponse.getAccessToken() == null) {
                log.error("Access token is null in response: {}", tokenResponse);
                throw new RuntimeException("Failed to exchange Google authorization code for access token");
            }

            log.info("Successfully exchanged code for access token");
            return tokenResponse;

        } catch (Exception e) {
            log.error("Failed to exchange authorization code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to exchange authorization code: " + e.getMessage());
        }
    }

    @Override
    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        try {
            log.info("Retrieving user information from Google");

            GoogleUserInfoResponse userInfo = googleUserInfoClient.getUserInfo("json", accessToken);

            if (userInfo.getEmail() == null) {
                throw new RuntimeException("Failed to retrieve user information from Google");
            }

            log.info("Successfully retrieved user info for email: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("Failed to retrieve user information: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user information: " + e.getMessage());
        }
    }

    @Override
    public String generateAuthorizationUrl(String state) {
        try {
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
            String encodedRedirectUri = URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8);

            return String.format(
                    "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&scope=openid%%20email%%20profile&response_type=code&state=%s&prompt=select_account",
                    googleClientId, encodedRedirectUri, encodedState);
        } catch (Exception e) {
            log.error("Failed to generate authorization URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate authorization URL: " + e.getMessage());
        }
    }
}
