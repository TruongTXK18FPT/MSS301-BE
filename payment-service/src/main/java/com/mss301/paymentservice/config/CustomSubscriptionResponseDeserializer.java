package com.mss301.paymentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.paymentservice.event.SubscriptionResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Custom deserializer for SubscriptionResponseEvent
 * Handles deserialization from both payment-service and premium-service
 * packages
 */
@Slf4j
public class CustomSubscriptionResponseDeserializer implements Deserializer<SubscriptionResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No configuration needed
    }

    @Override
    public SubscriptionResponseEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            // Deserialize to Map first to handle any package differences
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(data, Map.class);

            // Map to SubscriptionResponseEvent
            SubscriptionResponseEvent event = SubscriptionResponseEvent.builder()
                    .requestId((String) map.get("requestId"))
                    .subscriptionId(
                            map.get("subscriptionId") != null ? Long.valueOf(map.get("subscriptionId").toString())
                                    : null)
                    .userId(map.get("userId") != null ? Long.valueOf(map.get("userId").toString()) : null)
                    .planId(map.get("planId") != null ? Long.valueOf(map.get("planId").toString()) : null)
                    .planPrice(map.get("planPrice") != null ? Long.valueOf(map.get("planPrice").toString()) : null)
                    .subscriptionStatus((String) map.get("subscriptionStatus"))
                    .success(map.get("success") != null ? Boolean.valueOf(map.get("success").toString()) : false)
                    .errorMessage((String) map.get("errorMessage"))
                    .build();

            return event;
        } catch (Exception e) {
            log.error("Error deserializing SubscriptionResponseEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize SubscriptionResponseEvent", e);
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
