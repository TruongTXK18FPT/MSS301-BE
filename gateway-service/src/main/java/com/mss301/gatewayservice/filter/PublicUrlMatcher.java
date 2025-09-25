package com.mss301.gatewayservice.filter;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class PublicUrlMatcher {
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            // Authentication endpoints
            "/authenticate/login",
            "/authenticate/register",
            "/authenticate/introspect",
            "/authenticate/logout",
            "/authenticate/refresh",
            "/authenticate/verify-email",
            "/authenticate/forgot-password",
            "/authenticate/reset-password",
            "/authenticate/users/verify-otp",
            "/authenticate/users/resend",
            "/authenticate/users/forgot-password/reset",
            "/authenticate/users/forgot-password/verify",

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
            "/premium/premiums", // get all premiums
            "/content/contents", // get all contents
            "/profile/profiles" // get all profiles
            );

    private static final List<String> PUBLIC_WILDCARD_PATTERNS = List.of(
            "/premium/premiums/*", // get premium by id
            "/content/contents/*", // get content by id
            "/profile/profiles/*" // get profile by id
            );

    public boolean matches(String path) {
        return PUBLIC_EXACT_PATHS.contains(path)
                || PUBLIC_WILDCARD_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
