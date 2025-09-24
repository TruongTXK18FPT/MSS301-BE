package com.mss301.profileservice.dto.response;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {
    String userId;
    String fullName;
    LocalDate birthDate;
    String phone;
    String address;
    String gradeLevel;
    String specialty;
    Integer yearsOfExp;
    String certification;
    String relationship;
    String phoneAlt;
}
