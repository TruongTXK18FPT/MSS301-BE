package com.mss301.gatewayservice.service;

import org.springframework.stereotype.Service;

import com.mss301.gatewayservice.dto.ApiResponse;
import com.mss301.gatewayservice.dto.request.IntrospectRequest;
import com.mss301.gatewayservice.dto.response.IntrospectResponse;
import com.mss301.gatewayservice.repository.AuthenticationClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    AuthenticationClient authenticationClient;

    public Mono<ApiResponse<IntrospectResponse>> introspect(String token) {
        return authenticationClient.introspect(
                IntrospectRequest.builder().token(token).build());
    }
}
