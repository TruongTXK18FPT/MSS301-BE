package com.mss301.gatewayservice.filter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import com.mss301.gatewayservice.dto.ApiResponse;
import com.mss301.gatewayservice.service.AuthenticationService;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${app.api.prefix}")
    String API_PREFIX;

    final AuthenticationService authenticationService;
    final PublicUrlMatcher publicUrlMatcher;

    public AuthenticationFilter(AuthenticationService authenticationService, PublicUrlMatcher publicUrlMatcher) {
        this.authenticationService = authenticationService;
        this.publicUrlMatcher = publicUrlMatcher;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().replaceAll(API_PREFIX, "");

        if (publicUrlMatcher.matches(path)) {
            return chain.filter(exchange);
        }

        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authHeader)) {
            return unauthenticated(exchange.getResponse(), "Missing authorization header");
        }

        String token = authHeader.get(0).replace("Bearer ", "");
        log.info("Token: {}", token);

        return authenticationService
                .introspect(token)
                .flatMap(introspectResponse -> {
                    log.info("Introspection response: {}", introspectResponse);
                    String email = introspectResponse.getResult().getEmail();
                    String id = introspectResponse.getResult().getId();

                    ServerHttpRequest request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", id)
                            .header("X-User-Email", email)
                            .build();

                    ServerWebExchange mutatedExchange =
                            exchange.mutate().request(request).build();

                    if (introspectResponse.getResult().isValid()) {
                        return chain.filter(mutatedExchange);
                    } else {
                        return unauthenticated(exchange.getResponse(), "Invalid token");
                    }
                })
                .onErrorResume(throwable -> {
                    log.info("Introspection failed: {}", throwable.getMessage());
                    return unauthenticated(exchange.getResponse());
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> unauthenticated(ServerHttpResponse response) {
        return unauthenticated(response, "Unauthenticated");
    }

    private Mono<Void> unauthenticated(ServerHttpResponse response, String message) {
        ApiResponse<?> apiResponse =
                ApiResponse.builder().code(1401).message(message).build();

        String body =
                String.format("{\"code\":%d,\"message\":\"%s\"}", apiResponse.getCode(), apiResponse.getMessage());

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
