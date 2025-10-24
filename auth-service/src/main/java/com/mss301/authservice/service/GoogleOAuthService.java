package com.mss301.authservice.service;

import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;
import com.mss301.authservice.dto.response.GoogleUserInfoResponse;

/**
 * Service interface for Google OAuth operations
 * Follows Single Responsibility Principle (SRP)
 */
public interface GoogleOAuthService {

    /**
     * Exchange authorization code for access token
     *
     * @param code Authorization code from Google
     * @return GoogleOAuthTokenResponse containing access token
     */
    GoogleOAuthTokenResponse exchangeToken(String code);

    /**
     * Get user information from Google using access token
     *
     * @param accessToken Google access token
     * @return GoogleUserInfoResponse containing user details
     */
    GoogleUserInfoResponse getUserInfo(String accessToken);

    /**
     * Generate Google OAuth authorization URL
     *
     * @param state State parameter for security
     * @return Google OAuth authorization URL
     */
    String generateAuthorizationUrl(String state);
}
