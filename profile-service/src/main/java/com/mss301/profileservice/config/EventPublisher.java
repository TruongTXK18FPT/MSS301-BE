package com.mss301.profileservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import com.mss301.profileservice.event.UserProfileCreationFailedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Event publisher for profile-related events
 * Publishes events to Kafka topics using Spring Cloud Stream
 */
@Component
@Slf4j
public class EventPublisher {

    @Autowired
    private StreamBridge streamBridge;

    /**
     * Publishes UserProfileCreationFailedEvent when profile creation fails
     * @param event The event to publish containing userId and failure reason
     */
    public void publishUserProfileCreationFailedEvent(UserProfileCreationFailedEvent event) {
        try {
            boolean result = streamBridge.send("profileCreationFailed-out-0", event);
            if (result) {
                log.info("Successfully published UserProfileCreationFailedEvent for userId: {}", event.getUserId());
            } else {
                log.error("Failed to publish UserProfileCreationFailedEvent for userId: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error publishing UserProfileCreationFailedEvent for userId: {}", event.getUserId(), e);
        }
    }
}
