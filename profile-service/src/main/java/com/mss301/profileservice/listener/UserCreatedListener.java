package com.mss301.profileservice.listener;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mss301.profileservice.event.CreatedUserEvent;
import com.mss301.profileservice.service.ProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for user creation events
 * Handles CreatedUserEvent from auth-service using Spring Cloud Stream
 */
@Configuration
@Slf4j
public class UserCreatedListener {

    @Autowired
    private ProfileService profileService;

    /**
     * Consumer function to handle CreatedUserEvent
     * Creates user profile when user is created in auth-service
     * @return Consumer bean for Spring Cloud Stream
     */
    @Bean
    public Consumer<CreatedUserEvent> userCreated() {
        return event -> {
            try {
                log.info("Received CreatedUserEvent for user ID: {}", event.getId());
                log.debug("Creating profile for user: {}", event.getFullName());

                // Create user profile using the profile service
                profileService.createProfileFromUserEvent(event);

                log.info("Successfully processed CreatedUserEvent for user ID: {}", event.getId());
            } catch (Exception e) {
                log.error("Error processing CreatedUserEvent for user ID: {}", event.getId(), e);
                // The ProfileService will handle publishing failure events
            }
        };
    }
}
