package com.mss301.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mss301.authservice.dto.response.GoogleUserInfoResponse;

/**
 * Feign client for Google User Info API
 * Fetches user information using Google access token
 */
@FeignClient(name = "google-userinfo", url = "https://www.googleapis.com")
public interface GoogleUserInfoClient {

    /**
     * Get user information from Google
     *
     * @param alt         Response format (json)
     * @param accessToken Google access token
     * @return GoogleUserInfoResponse containing user details
     */
    @GetMapping(value = "/oauth2/v1/userinfo")
    GoogleUserInfoResponse getUserInfo(
            @RequestParam("alt") String alt, @RequestParam("access_token") String accessToken);
}
