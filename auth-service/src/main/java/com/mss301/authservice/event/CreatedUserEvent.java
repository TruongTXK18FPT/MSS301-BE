package com.mss301.authservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatedUserEvent {
    String id;
    String email;
    String fullName; // User full name from registration
    String userType; // STUDENT, TEACHER, GUARDIAN
}
