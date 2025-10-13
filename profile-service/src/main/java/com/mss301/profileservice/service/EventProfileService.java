package com.mss301.profileservice.service;

import com.mss301.profileservice.event.CreatedUserEvent;
import com.mss301.profileservice.event.ProfileCompletedEvent;

/**
 * Service interface for event-driven profile operations
 * Handles automatic profile creation from user events
 */
public interface EventProfileService {

    /**
     * Create a profile from a user creation event
     *
     * @param event The user creation event
     */
    void createProfileFromUserEvent(CreatedUserEvent event);

    /**
     * Create a teacher profile from a user creation event
     *
     * @param userId User ID
     * @param event  The user creation event
     */
    void createTeacherProfile(Long userId, CreatedUserEvent event);

    /**
     * Create a guardian profile from a user creation event
     *
     * @param userId User ID
     * @param event  The user creation event
     */
    void createGuardianProfile(Long userId, CreatedUserEvent event);

    /**
     * Complete user profile from ProfileCompletedEvent
     * Updates the profile with role-specific data
     *
     * @param event The profile completion event
     */
    void completeProfileFromEvent(ProfileCompletedEvent event);
}
