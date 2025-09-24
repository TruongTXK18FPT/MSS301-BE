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
public class UserProfileRequest {
    private String fullName;
    private LocalDate dob;
    private String phoneNumber;
    private String address;
    private String bio;
    private String avatarUrl;
}
