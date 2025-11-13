package com.mss301.premiumservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.premiumservice.event.SubscriptionRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Custom deserializer for SubscriptionRequestEvent
 * Handles deserialization from both payment-service and premium-service
 * packages
 */
@Slf4j
public class CustomSubscriptionRequestDeserializer implements Deserializer<SubscriptionRequestEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No configuration needed
    }

    @Override
    public SubscriptionRequestEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            // Deserialize to Map first to handle any package differences
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(data, Map.class);

            // Map to SubscriptionRequestEvent
            SubscriptionRequestEvent event = SubscriptionRequestEvent.builder()
                    .requestId((String) map.get("requestId"))
                    .subscriptionId(
                            map.get("subscriptionId") != null ? Long.valueOf(map.get("subscriptionId").toString())
                                    : null)
                    .replyTopic((String) map.get("replyTopic"))
                    .build();

            return event;
        } catch (Exception e) {
            log.error("Error deserializing SubscriptionRequestEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize SubscriptionRequestEvent", e);
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
