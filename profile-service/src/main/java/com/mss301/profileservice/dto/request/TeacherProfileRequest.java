package com.mss301.profileservice.dto.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherProfileRequest {
    private String fullName;
    private LocalDate dob;
    private String phoneNumber;
    private String address;
    private String department;
    private String specialization;
    private Integer yearsOfExperience;
    private String qualifications;
    private String bio;
}
