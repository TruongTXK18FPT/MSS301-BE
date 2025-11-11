package com.mss301.contentservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class GenerateQuizRequest {
    @NotBlank(message = "Topic is required")
    private String topic;

    @NotBlank(message = "Grade is required")
    private String grade;

    @NotNull(message = "Number of questions is required")
    @Min(value = 1, message = "Must generate at least 1 question")
    @Max(value = 20, message = "Cannot generate more than 20 questions at once")
    private Integer numQuestions;
}
