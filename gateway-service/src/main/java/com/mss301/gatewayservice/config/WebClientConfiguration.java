package com.mss301.gatewayservice.config;

import java.util.List;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.mss301.gatewayservice.repository.AuthenticationClient;

@Configuration
public class WebClientConfiguration {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient webClient(WebClient.Builder loadBalancedWebClientBuilder) {
        // Use lb:// to leverage Spring Cloud LoadBalancer
        // This will automatically resolve service name to correct host:port via Eureka
        return loadBalancedWebClientBuilder.baseUrl("lb://auth-service")
                .build();
    }

    /**
     * CORS configuration for local development only.
     * In production (profile: prod), CORS is configured via spring.cloud.gateway.globalcors
     * in application-prod.yml to avoid duplicate headers.
     */
    @Bean
    @Profile("!prod")
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Allow specific origins for local development
        corsConfiguration.setAllowedOriginPatterns(List.of(
                "https://mss301.me",
                "https://*.mss301.me",
                "http://localhost:*",
                "http://127.0.0.1:*"));

        corsConfiguration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));

        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        // Add exposed headers to allow frontend to read response headers
        corsConfiguration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }

    @Bean
    public AuthenticationClient authenticationClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(
                WebClientAdapter.create(webClient))
                .build();

        return httpServiceProxyFactory.createClient(AuthenticationClient.class);
    }
}
