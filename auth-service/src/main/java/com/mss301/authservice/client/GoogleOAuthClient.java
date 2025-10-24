package com.mss301.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     * @param formData Form data containing OAuth parameters
     * @return GoogleOAuthTokenResponse containing access token
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    GoogleOAuthTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> formData);
}
