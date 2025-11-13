package com.mss301.contentservice.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class QuizRequestPayload {

    private QuizRequestPayload() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizRequest {
        private Integer timeLimitSec;
        private Boolean shuffleQuestions;

        @NotNull
        private List<QuizQuestionRequest> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionRequest {
        @NotBlank
        private String text;

        private Integer points;

        @NotBlank
        private String type; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER

        private String explanation; // Giải thích đáp án

        private List<QuizOptionRequest> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOptionRequest {
        @NotBlank
        private String text;

        @NotNull
        private Boolean correct;
    }
}
