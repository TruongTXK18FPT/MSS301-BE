package com.mss301.classroomservice.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private String joinCode;
    private String password;
    private Integer maxStudents;
    private Integer currentStudents;
    private Integer assignmentCount;
    private Integer quizCount;
    private Integer contentCount; // For lessons/mindmaps
    private Long ownerId;
    private String subject; // Môn học
    private String grade; // Khối lớp

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
