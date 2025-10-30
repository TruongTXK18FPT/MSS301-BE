package com.mss301.authservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    String fullName;
    String email;
    String password;
    String confirmPassword;
    String userType; // STUDENT, TEACHER, GUARDIAN

    // Teacher-specific fields (optional, only for TEACHER userType)
    String department;
    String specialization;
    Integer yearsOfExperience;
    String qualifications;
    String bio;
    String phone;
}
