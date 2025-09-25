package com.mss301.gatewayservice.repository;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import com.mss301.gatewayservice.dto.ApiResponse;
import com.mss301.gatewayservice.dto.request.IntrospectRequest;
import com.mss301.gatewayservice.dto.response.IntrospectResponse;

import reactor.core.publisher.Mono;

public interface AuthenticationClient {
    @PostExchange(url = "/auth/introspect", contentType = MediaType.APPLICATION_JSON_VALUE)
    Mono<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request);
}
