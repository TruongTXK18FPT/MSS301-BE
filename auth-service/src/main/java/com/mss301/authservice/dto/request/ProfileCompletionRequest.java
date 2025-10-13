package com.mss301.authservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileCompletionRequest {
    String userType; // STUDENT, TEACHER, GUARDIAN
    Object data; // Will be deserialized to specific request based on userType
}
