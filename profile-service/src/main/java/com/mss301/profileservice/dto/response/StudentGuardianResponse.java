package com.mss301.profileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardianResponse {
    private Long studentId;
    private Long guardianId;
    private StudentProfileResponse student;
    private GuardianProfileResponse guardian;
}
