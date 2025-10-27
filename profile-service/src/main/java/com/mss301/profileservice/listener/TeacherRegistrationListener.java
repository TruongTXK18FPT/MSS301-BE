package com.mss301.profileservice.listener;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mss301.profileservice.event.TeacherRegistrationEvent;
import com.mss301.profileservice.service.EventProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for teacher registration events
 * Handles TeacherRegistrationEvent from auth-service using Spring Cloud Stream
 */
@Configuration
@Slf4j
public class TeacherRegistrationListener {

    @Autowired
    private EventProfileService eventProfileService;

    /**
     * Consumer function to handle TeacherRegistrationEvent
     * Creates teacher profile when a teacher registers
     *
     * @return Consumer bean for Spring Cloud Stream
     */
    @Bean
    public Consumer<TeacherRegistrationEvent> teacherRegistration() {
        return event -> {
            try {
                log.info("Received TeacherRegistrationEvent for user ID: {}", event.getId());
                log.debug(
                        "Creating teacher profile for user: {} ({}) - {}",
                        event.getId(),
                        event.getEmail(),
                        event.getFullName());

                // Create teacher profile using the event profile service
                eventProfileService.createTeacherProfileFromEvent(event);

                log.info("Successfully processed TeacherRegistrationEvent for user ID: {}", event.getId());
            } catch (Exception e) {
                log.error("Error processing TeacherRegistrationEvent for user ID: {}", event.getId(), e);
            }
        };
    }
}
