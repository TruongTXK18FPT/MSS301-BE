package com.mss301.authservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileCompletedEvent {
    String userId;
    String userType; // STUDENT, TEACHER, GUARDIAN
    Object data; // JSON data containing profile completion info
}
