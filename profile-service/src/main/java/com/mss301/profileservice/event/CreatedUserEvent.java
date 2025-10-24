package com.mss301.profileservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatedUserEvent {
    String id; // User ID from auth-service
    String email; // User email
    String fullName; // User full name from registration
    String userType; // STUDENT, TEACHER, GUARDIAN
}
