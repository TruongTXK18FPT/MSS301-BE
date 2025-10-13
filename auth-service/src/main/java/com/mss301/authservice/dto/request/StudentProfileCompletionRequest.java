package com.mss301.authservice.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentProfileCompletionRequest {
    String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthDate;

    String school;
    String grade;
    String learningGoals;
    String subjectsOfInterest;
}
