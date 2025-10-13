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
public class QuizResponsePayload {
    private Integer timeLimitSec;
    private Boolean shuffleQuestions;
    private List<QuizQuestionDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionDto {
        private Long id;
        private String text;
        private Integer points;
        private String type;
        private List<QuizOptionDto> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOptionDto {
        private Long id;
        private String text;
        private Boolean correct;
    }
}
