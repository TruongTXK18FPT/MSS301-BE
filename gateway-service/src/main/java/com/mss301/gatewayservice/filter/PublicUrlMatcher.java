package com.mss301.gatewayservice.filter;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class PublicUrlMatcher {
        private static final AntPathMatcher pathMatcher = new AntPathMatcher();

        private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
                        // Health check endpoints
                        "/actuator/health",
                        "/api/v1/authenticate/health",
                        "/health",

                        // Authentication endpoints (with /api/v1 prefix)
                        "/api/v1/authenticate/auth/login",
                        "/api/v1/authenticate/auth/introspect",
                        "/api/v1/authenticate/auth/logout",
                        "/api/v1/authenticate/auth/refresh",
                        "/api/v1/authenticate/auth/verify-email",
                        "/api/v1/authenticate/auth/reset-password",
                        "/api/v1/authenticate/auth/google/redirect",
                        "/api/v1/authenticate/auth/google/callback",
                        "/api/v1/authenticate/auth/password-setup-status",
                        "/api/v1/authenticate/auth/send-email-verification",
                        "/api/v1/authenticate/auth/resend-email-verification",
                        "/api/v1/authenticate/auth/otp-info",
                        "/api/v1/authenticate/auth/send-password-reset",

                        // User endpoints (with /api/v1 prefix)
                        "/api/v1/authenticate/users/register",
                        "/api/v1/authenticate/users/verify-otp",
                        "/api/v1/authenticate/users/resend",
                        "/api/v1/authenticate/users/forgot-password/reset",
                        "/api/v1/authenticate/users/forgot-password/verify",
                        "/api/v1/authenticate/users/my-profile-status",

                        // Authentication endpoints (AuthenticationController with /auth prefix - legacy
                        // paths)
                        "/authenticate/auth/login",
                        "/authenticate/auth/introspect",
                        "/authenticate/auth/logout",
                        "/authenticate/auth/refresh",
                        "/authenticate/auth/verify-email",
                        "/authenticate/auth/reset-password",
                        "/authenticate/auth/google/redirect",
                        "/authenticate/auth/google/callback",
                        "/authenticate/auth/password-setup-status",
                        "/authenticate/auth/send-email-verification",
                        "/authenticate/auth/resend-email-verification",
                        "/authenticate/auth/otp-info",
                        "/authenticate/auth/send-password-reset",

                        // User endpoints (UserController with /users prefix - legacy paths)
                        "/authenticate/users/register",
                        "/authenticate/users/verify-otp",
                        "/authenticate/users/resend",
                        "/authenticate/users/forgot-password/reset",
                        "/authenticate/users/forgot-password/verify",
                        "/authenticate/users/my-profile-status",

                        // Test endpoints
                        "/authenticate/test",
                        "/payment/test",
                        "/premium/test",
                        "/mindmap/test",
                        "/content/test",
                        "/chatbot/test",
                        "/profile/test",
                        "/notification/test",

                        // Public content endpoints
                        "/premium/premiums",
                        "/content/contents",
                        "/profile/profiles");

        private static final List<String> PUBLIC_WILDCARD_PATTERNS = List.of(
                        // Actuator endpoints
                        "/actuator/**",
                        // Premium/Content/Profile public endpoints
                        "/api/v1/premium/premiums/*",
                        "/api/v1/content/contents/*",
                        "/api/v1/profile/profiles/*",
                        "/premium/premiums/*",
                        "/content/contents/*",
                        "/profile/profiles/*");

        public boolean isPublicUrl(String path) {
                boolean isExactMatch = PUBLIC_EXACT_PATHS.contains(path);
                boolean isWildcardMatch = PUBLIC_WILDCARD_PATTERNS.stream()
                                .anyMatch(pattern -> pathMatcher.match(pattern, path));

                // Debug log for otp-info endpoint
                if (path != null && path.contains("otp-info")) {
                        System.out.println("DEBUG PublicUrlMatcher - Checking path: " + path);
                        System.out.println("DEBUG PublicUrlMatcher - isExactMatch: " + isExactMatch);
                        System.out.println("DEBUG PublicUrlMatcher - isWildcardMatch: " + isWildcardMatch);
                        System.out.println("DEBUG PublicUrlMatcher - PUBLIC_EXACT_PATHS contains: "
                                        + PUBLIC_EXACT_PATHS.contains(path));
                        System.out.println("DEBUG PublicUrlMatcher - PUBLIC_EXACT_PATHS: " + PUBLIC_EXACT_PATHS);
                }

                return isExactMatch || isWildcardMatch;
        }
}
