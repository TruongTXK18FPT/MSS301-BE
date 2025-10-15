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
    String fullName;
    String username;
    String userType; // STUDENT, TEACHER, GUARDIAN
}
