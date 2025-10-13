package com.mss301.profileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletionStatusResponse {
    private boolean profileCompleted;
    private String userType; // STUDENT, TEACHER, GUARDIAN
    private String username;
    private String email;
}
