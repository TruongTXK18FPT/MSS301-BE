package com.mss301.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentItemRequest {
    @NotNull
    private String type; // LESSON, ASSIGNMENT, QUIZ, RESOURCE

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private String content;

    private String subject;
    private String grade;
    private String tags;

    private Boolean isPublic = false;
}
