package com.mss301.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import com.mss301.authservice.event.CreatedUserEvent;
import com.mss301.authservice.event.NotificationEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Event publisher for auth-service related events
 * Publishes events to Kafka topics using Spring Cloud Stream
 */
@Component
@Slf4j
public class EventPublisher {

    @Autowired
    private StreamBridge streamBridge;

    /**
     * Publishes CreatedUserEvent when a new user is registered
     *
     * @param event The event to publish containing user details
     */
    public void publishCreatedUserEvent(CreatedUserEvent event) {
        try {
            boolean result = streamBridge.send("userCreated-out-0", event);
            if (result) {
                log.info("Successfully published CreatedUserEvent for user ID: {}", event.getId());
            } else {
                log.error("Failed to publish CreatedUserEvent for user ID: {}", event.getId());
            }
        } catch (Exception e) {
            log.error("Error publishing CreatedUserEvent for user ID: {}", event.getId(), e);
        }
    }

    /**
     * Publishes NotificationEvent for sending emails/notifications
     *
     * @param event The notification event to publish
     */
    public void publishNotificationEvent(NotificationEvent event) {
        try {
            boolean result = streamBridge.send("sendNotification-out-0", event);
            if (result) {
                log.info("Successfully published NotificationEvent to: {}", event.getRecipient());
            } else {
                log.error("Failed to publish NotificationEvent to: {}", event.getRecipient());
            }
        } catch (Exception e) {
            log.error("Error publishing NotificationEvent to: {}", event.getRecipient(), e);
        }
    }
}
