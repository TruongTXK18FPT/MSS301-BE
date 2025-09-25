package com.mss301.gatewayservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);

        map.put("timestamp", LocalDateTime.now());
        map.put("service", "gateway-service");

        // Remove server information for security
        map.remove("trace");

        return map;
    }
}
