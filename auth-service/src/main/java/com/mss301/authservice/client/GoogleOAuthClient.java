package com.mss301.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;

/**
 * Feign client for Google OAuth2 token exchange
 * Handles authorization code to access token exchange
 */
@FeignClient(name = "google-oauth", url = "https://oauth2.googleapis.com")
public interface GoogleOAuthClient {

    /**
     * Exchange authorization code for access token
     *
     * @param code         Authorization code from Google
     * @param clientId     Google OAuth2 client ID
     * @param clientSecret Google OAuth2 client secret
     * @param redirectUri  Redirect URI configured in Google Console
     * @param grantType    Grant type (authorization_code)
     * @return GoogleOAuthTokenResponse containing access token
     */
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleOAuthTokenResponse exchangeToken(
            @RequestParam("code") String code,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("grant_type") String grantType);
}
