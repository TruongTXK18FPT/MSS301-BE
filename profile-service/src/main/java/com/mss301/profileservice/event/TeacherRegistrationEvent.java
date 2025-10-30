package com.mss301.profileservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Event received when a teacher registers
 * Contains all teacher-specific registration data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherRegistrationEvent {
    String id; // User ID
    String email;
    String fullName;

    // Teacher-specific fields
    String department;
    String specialization;
    Integer yearsOfExperience;
    String qualifications;
    String bio;
    String phone;
}
