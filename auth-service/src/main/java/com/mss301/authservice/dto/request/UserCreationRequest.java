package com.mss301.authservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    String username;
    String email;
    String password;
    String confirmPassword;
    String phone;
    Long tenantId;
    String userType; // STUDENT, TEACHER, GUARDIAN

    // Additional profile fields based on user type
    String fullName;
    String address;
    Integer districtCode;
    Integer provinceCode;
}
