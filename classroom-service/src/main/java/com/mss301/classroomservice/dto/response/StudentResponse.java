package com.mss301.classroomservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long userId;
    private String email;
    private String fullName;
    private LocalDateTime joinedAt;
    private String role; // STUDENT
}
