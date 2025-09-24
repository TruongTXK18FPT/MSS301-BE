package com.mss301.profileservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private LocalDate dob;
    private String phoneNumber;
    private String address;
    private String department;
    private String specialization;
    private Integer yearsOfExperience;
    private String qualifications;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
