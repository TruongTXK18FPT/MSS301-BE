package com.mss301.gatewayservice.filter;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class PublicUrlMatcher {
        private static final AntPathMatcher pathMatcher = new AntPathMatcher();

        private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
                        // Authentication endpoints (AuthenticationController with /auth prefix)
                        "/authenticate/auth/login",
                        "/authenticate/auth/introspect",
                        "/authenticate/auth/logout",
                        "/authenticate/auth/refresh",
                        "/authenticate/auth/verify-email",
                        "/authenticate/auth/reset-password",
                        "/authenticate/auth/google/redirect",
                        "/authenticate/auth/google/callback",
                        "/authenticate/auth/google/setup-password",
                        "/authenticate/auth/password-setup-status",
                        "/authenticate/auth/send-email-verification",
                        "/authenticate/auth/send-password-reset",

                        // User endpoints (UserController with /users prefix)
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

        private static final List<String> PUBLIC_WILDCARD_PATTERNS = List.of("/premium/premiums/*",
                        "/content/contents/*", "/profile/profiles/*");

        public boolean isPublicUrl(String path) {
                return PUBLIC_EXACT_PATHS.contains(path)
                                || PUBLIC_WILDCARD_PATTERNS.stream()
                                                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        }
}
