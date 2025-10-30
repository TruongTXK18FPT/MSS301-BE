package com.mss301.contentservice.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {
    private Long id;
    private Long quizId;
    private String questionText;
    private String questionType;
    private Integer points;
    private String explanation;
    private Integer orderIndex;
    private List<QuizOptionResponse> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOptionResponse {
        private Long id;
        private String optionText;
        private Boolean isCorrect; // Only show to teachers
        private Integer orderIndex;
    }
}
