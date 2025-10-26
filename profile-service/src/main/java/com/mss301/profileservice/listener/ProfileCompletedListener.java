package com.mss301.profileservice.listener;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mss301.profileservice.event.ProfileCompletedEvent;
import com.mss301.profileservice.service.EventProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for profile completion events
 * Handles ProfileCompletedEvent from auth-service using Spring Cloud Stream
 */
@Configuration
@Slf4j
public class ProfileCompletedListener {

    @Autowired
    private EventProfileService eventProfileService;

    /**
     * Consumer function to handle ProfileCompletedEvent
     * Updates user profile when user completes their profile in auth-service
     * 
     * @return Consumer bean for Spring Cloud Stream
     */
    @Bean
    public Consumer<ProfileCompletedEvent> profileCompleted() {
        return event -> {
            try {
                log.info(
                        "Received ProfileCompletedEvent for user ID: {} with type: {}",
                        event.getUserId(),
                        event.getUserType());

                // Update profile completion using the event profile service
                eventProfileService.completeProfileFromEvent(event);

                log.info("Successfully processed ProfileCompletedEvent for user ID: {}", event.getUserId());
            } catch (Exception e) {
                log.error("Error processing ProfileCompletedEvent for user ID: {}", event.getUserId(), e);
            }
        };
    }
}
