package com.mss301.contentservice.dto.request;

import java.util.List;

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
public class QuizQuestionRequest {
    
    @NotBlank(message = "Question text is required")
    private String questionText;
    
    @NotNull(message = "Question type is required")
    private QuestionType questionType;
    
    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;
    
    private String explanation;
    private List<QuizOptionRequest> options;
    private Integer orderIndex;

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        SHORT_ANSWER,
        ESSAY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOptionRequest {
        @NotBlank(message = "Option text is required")
        private String optionText;
        
        @NotNull(message = "isCorrect flag is required")
        private Boolean isCorrect;
        
        private Integer orderIndex;
    }
}
