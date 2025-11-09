package com.mss301.authservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyEmailResponse {
    String userType; // STUDENT, TEACHER, GUARDIAN
    boolean emailVerified;
}
