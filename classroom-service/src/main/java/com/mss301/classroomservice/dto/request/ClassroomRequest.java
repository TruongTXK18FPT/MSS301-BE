package com.mss301.classroomservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
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

    @Default
    private Boolean isPublic = false;

    @Size(max = 255)
    private String password;

    private String joinCode;

    @Default
    private Integer maxStudents = 50; // Số học sinh tối đa

    @Default
    @Size(max = 100)
    private String subject = "Toán học"; // Môn học, mặc định là Toán học

    @Size(max = 100)
    private String grade; // Khối lớp (1-12)
}
