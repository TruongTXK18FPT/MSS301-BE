package com.mss301.authservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mss301.authservice.client.GoogleOAuthClient;
import com.mss301.authservice.client.GoogleUserInfoClient;
import com.mss301.authservice.dto.response.GoogleOAuthTokenResponse;
import com.mss301.authservice.dto.response.GoogleUserInfoResponse;
import com.mss301.authservice.service.impl.GoogleOAuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private GoogleUserInfoClient googleUserInfoClient;

    @InjectMocks
    private GoogleOAuthServiceImpl googleOAuthService;

    private String testCode;
    private String testAccessToken;
    private GoogleOAuthTokenResponse tokenResponse;
    private GoogleUserInfoResponse userInfoResponse;

    @BeforeEach
    void setUp() {
        testCode = "test_authorization_code";
        testAccessToken = "test_access_token";

        tokenResponse = GoogleOAuthTokenResponse.builder()
                .accessToken(testAccessToken)
                .expiresIn(3600L)
                .build();

        userInfoResponse = GoogleUserInfoResponse.builder()
                .id("test_google_id")
                .email("test@example.com")
                .name("Test User")
                .givenName("Test")
                .familyName("User")
                .verifiedEmail(true)
                .build();
    }

    @Test
    void testExchangeToken_Success() {
        // Given
        when(googleOAuthClient.exchangeToken(any())).thenReturn(tokenResponse);

        // When
        GoogleOAuthTokenResponse result = googleOAuthService.exchangeToken(testCode);

        // Then
        assertNotNull(result);
        assertEquals(testAccessToken, result.getAccessToken());
        verify(googleOAuthClient).exchangeToken(any());
    }

    @Test
    void testExchangeToken_Failure() {
        // Given
        when(googleOAuthClient.exchangeToken(any()))
                .thenReturn(GoogleOAuthTokenResponse.builder().build());

        // When & Then
        assertThrows(RuntimeException.class, () -> googleOAuthService.exchangeToken(testCode));
    }

    @Test
    void testGetUserInfo_Success() {
        // Given
        when(googleUserInfoClient.getUserInfo(anyString(), anyString())).thenReturn(userInfoResponse);

        // When
        GoogleUserInfoResponse result = googleOAuthService.getUserInfo(testAccessToken);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
        verify(googleUserInfoClient).getUserInfo("json", testAccessToken);
    }

    @Test
    void testGetUserInfo_Failure() {
        // Given
        when(googleUserInfoClient.getUserInfo(anyString(), anyString()))
                .thenReturn(GoogleUserInfoResponse.builder().build());

        // When & Then
        assertThrows(RuntimeException.class, () -> googleOAuthService.getUserInfo(testAccessToken));
    }

    @Test
    void testGenerateAuthorizationUrl() {
        // When
        String result = googleOAuthService.generateAuthorizationUrl("test_state");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("accounts.google.com"));
        assertTrue(result.contains("test_state"));
    }
}
