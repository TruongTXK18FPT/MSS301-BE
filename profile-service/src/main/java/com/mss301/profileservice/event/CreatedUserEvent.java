package com.mss301.profileservice.event;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatedUserEvent {
    String id;
    String fullName;
    String userType; // STUDENT, TEACHER, GUARDIAN

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthDate;

    String phone;
    String address;
    Integer districtCode;
    Integer provinceCode;
}
