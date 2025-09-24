package com.mss301.gatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final WebClient webClient;

    public AuthenticationGatewayFilterFactory(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip authentication for public endpoints
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            return validateToken(token)
                    .flatMap(userId -> {
                        if (userId != null) {
                            // Add user ID to headers for downstream services
                            var mutatedRequest = exchange.getRequest()
                                    .mutate()
                                    .header("X-User-Id", userId)
                                    .build();
                            var mutatedExchange =
                                    exchange.mutate().request(mutatedRequest).build();
                            return chain.filter(mutatedExchange);
                        } else {
                            log.warn("Token validation failed for path: {}", path);
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                    })
                    .onErrorResume(throwable -> {
                        log.error("Error during token validation: {}", throwable.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/authenticate/login")
                || path.startsWith("/api/v1/authenticate/register")
                || path.startsWith("/api/v1/authenticate/refresh")
                || path.startsWith("/api/v1/authenticate/verify-email")
                || path.startsWith("/api/v1/authenticate/forgot-password")
                || path.startsWith("/api/v1/authenticate/reset-password")
                || path.startsWith("/eureka/")
                || path.startsWith("/actuator/");
    }

    private Mono<String> validateToken(String token) {
        return webClient
                .post()
                .uri("http://auth-service/introspect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.info("Token validation response: {}", response);
                    // Simple check for successful response - should parse JSON properly
                    if (response.contains("\"valid\":true")) {
                        // Extract userId from response - this is simplified
                        String userIdPattern = "\"userId\":\"";
                        int startIndex = response.indexOf(userIdPattern);
                        if (startIndex != -1) {
                            startIndex += userIdPattern.length();
                            int endIndex = response.indexOf("\"", startIndex);
                            if (endIndex != -1) {
                                return response.substring(startIndex, endIndex);
                            }
                        }
                        return "authenticated-user";
                    }
                    return null;
                })
                .onErrorResume(error -> {
                    log.error("Token validation error: {}", error.getMessage());
                    return Mono.just(null);
                });
    }

    public static class Config {
        // Configuration properties if needed
    }
}
