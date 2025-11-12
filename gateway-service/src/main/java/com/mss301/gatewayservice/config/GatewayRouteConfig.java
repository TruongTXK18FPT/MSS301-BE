package com.mss301.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 * 
 * Reason: Spring Cloud Gateway Server Webflux 4.3.0 (2025.0.0) does not support
 * shorthand YAML syntax (Path=..., RewritePath=...) in production JAR runtime.
 * 
 * Using Java-based RouteLocator provides consistent behavior across dev and
 * prod.
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service: /api/v1/authenticate/** → /auth/**
                .route("auth-service", r -> r
                        .path("/api/v1/authenticate/**")
                        .filters(f -> f.rewritePath("/api/v1/authenticate/(?<segment>.*)", "/$\\{segment}"))
                        .uri("lb://auth-service"))

                // Payment Service: /api/v1/payment/** → /payment/**
                .route("payment-service", r -> r
                        .path("/api/v1/payment/**")
                        .filters(f -> f.rewritePath("/api/v1/payment/(?<segment>.*)", "/$\\{segment}"))
                        .uri("lb://payment-service"))

                // Premium Service: /api/v1/premium/** → routes to /api/v1/plans/** and
                // /api/v1/entitlements/**
                .route("premium-service-plans", r -> r
                        .path("/api/v1/premium/plans/**")
                        .filters(f -> f.rewritePath("/api/v1/premium/plans/(?<segment>.*)",
                                "/api/v1/plans/$\\{segment}"))
                        .uri("lb://premium-service"))
                .route("premium-service-entitlements", r -> r
                        .path("/api/v1/premium/entitlements/**")
                        .filters(f -> f.rewritePath("/api/v1/premium/entitlements/(?<segment>.*)",
                                "/api/v1/entitlements/$\\{segment}"))
                        .uri("lb://premium-service"))

                // Mindmap Service: /api/v1/mindmap/** → strip 3 segments
                .route("mindmap-service", r -> r
                        .path("/api/v1/mindmap/**")
                        .filters(f -> f.stripPrefix(3))
                        .uri("lb://mindmap-service"))

                // Content Service: multiple paths → strip 3 segments
                .route("content-service", r -> r
                        .path("/api/v1/content/**", "/api/v1/submissions/**", "/api/v1/quiz-attempts/**")
                        .filters(f -> f.stripPrefix(3))
                        .uri("lb://content-service"))

                // Chatbot Service: /api/v1/chatbot/** → /chatbot/**
                .route("chatbot-service", r -> r
                        .path("/api/v1/chatbot/**")
                        .filters(f -> f.rewritePath("/api/v1/chatbot/(?<segment>.*)", "/$\\{segment}"))
                        .uri("lb://chatbot-service"))

                // Profile Service: /api/v1/profile/** → /profile/**
                .route("profile-service", r -> r
                        .path("/api/v1/profile/**")
                        .filters(f -> f.rewritePath("/api/v1/profile/(?<segment>.*)", "/$\\{segment}"))
                        .uri("lb://profile-service"))

                // Classroom Service: /api/v1/classrooms/** → strip 3 segments
                .route("classroom-service", r -> r
                        .path("/api/v1/classrooms/**")
                        .filters(f -> f.stripPrefix(3))
                        .uri("lb://classroom-service"))

                // Media Service: /api/v1/media/** → no rewrite (StripPrefix=0)
                .route("media-service", r -> r
                        .path("/api/v1/media/**")
                        .uri("lb://media-service"))

                // Document Service: /api/v1/document/** → no rewrite (service expects
                // /api/v1/documents/**)
                .route("document-service", r -> r
                        .path("/api/v1/document/**")
                        .filters(
                                f -> f.rewritePath("/api/v1/document/(?<segment>.*)", "/api/v1/documents/$\\{segment}"))
                        .uri("lb://document-service"))

                // Retrieval Service: /api/v1/retrieval/** → no rewrite (service expects
                // /api/v1/retrieval/**)
                .route("retrieval-service", r -> r
                        .path("/api/v1/retrieval/**")
                        .uri("lb://retrieval-service"))

                // RAG Service: /api/v1/rag/** → no rewrite (service expects /api/v1/rag/** or
                // /api/v1/tts/**)
                .route("rag-service", r -> r
                        .path("/api/v1/rag/**", "/api/v1/tts/**")
                        .uri("lb://rag-service"))

                // Notification Service: /api/v1/notification/** → strip 3 segments
                .route("notification-service", r -> r
                        .path("/api/v1/notification/**")
                        .filters(f -> f.stripPrefix(3))
                        .uri("lb://notification-service"))

                .build();
    }
}
