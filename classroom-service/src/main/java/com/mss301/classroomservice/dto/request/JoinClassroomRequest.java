package com.mss301.classroomservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinClassroomRequest {
    @NotBlank
    private String classroomCode; // Mã lớp học
    
    @NotBlank
    private String password; // Mật khẩu lớp học
}
