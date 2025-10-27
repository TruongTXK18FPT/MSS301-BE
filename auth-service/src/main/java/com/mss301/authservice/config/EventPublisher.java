package com.mss301.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import com.mss301.authservice.event.CreatedUserEvent;
import com.mss301.authservice.event.NotificationEvent;
import com.mss301.authservice.event.ProfileCompletedEvent;
import com.mss301.authservice.event.TeacherApprovalEvent;
import com.mss301.authservice.event.TeacherRegistrationEvent;

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

    /**
     * Publishes ProfileCompletedEvent when a user completes their profile
     *
     * @param event The event to publish containing profile completion details
     */
    public void publishProfileCompletedEvent(ProfileCompletedEvent event) {
        try {
            boolean result = streamBridge.send("profileCompleted-out-0", event);
            if (result) {
                log.info("Successfully published ProfileCompletedEvent for user ID: {}", event.getUserId());
            } else {
                log.error("Failed to publish ProfileCompletedEvent for user ID: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error publishing ProfileCompletedEvent for user ID: {}", event.getUserId(), e);
        }
    }

    /**
     * Publishes TeacherRegistrationEvent when a teacher registers
     *
     * @param event The event to publish containing teacher registration details
     */
    public void publishTeacherRegistrationEvent(TeacherRegistrationEvent event) {
        try {
            boolean result = streamBridge.send("teacherRegistration-out-0", event);
            if (result) {
                log.info("Successfully published TeacherRegistrationEvent for user ID: {}", event.getId());
            } else {
                log.error("Failed to publish TeacherRegistrationEvent for user ID: {}", event.getId());
            }
        } catch (Exception e) {
            log.error("Error publishing TeacherRegistrationEvent for user ID: {}", event.getId(), e);
        }
    }

    /**
     * Publishes TeacherApprovalEvent when admin approves/rejects teacher
     *
     * @param event The event to publish containing teacher approval details
     */
    public void publishTeacherApprovalEvent(TeacherApprovalEvent event) {
        try {
            boolean result = streamBridge.send("teacherApproval-out-0", event);
            if (result) {
                log.info("Successfully published TeacherApprovalEvent for user ID: {}", event.getUserId());
            } else {
                log.error("Failed to publish TeacherApprovalEvent for user ID: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error publishing TeacherApprovalEvent for user ID: {}", event.getUserId(), e);
        }
    }
}
