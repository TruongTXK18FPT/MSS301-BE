package com.mss301.profileservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Event published when a user completes their profile
 * Contains all role-specific profile data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileCompletedEvent {
    String userId; // User ID
    String userType; // STUDENT, TEACHER, GUARDIAN
    Object data; // Role-specific data (StudentProfileCompletionRequest,
    // TeacherProfileCompletionRequest,
    // GuardianProfileCompletionRequest)
}
