package com.mss301.classroomservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    private Boolean isPublic = false;
    
    @Size(max = 255)
    private String password; // Mật khẩu để vào lớp
    
    private Integer maxStudents = 50; // Số học sinh tối đa
}
