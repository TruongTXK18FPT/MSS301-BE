package com.mss301.ragservice.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapGenerationResponse {
    private String centralTopic;
    private String grade;
    private String subject;
    private String difficulty;
    private String estimatedTime;
    private List<String> prerequisites;
    private List<Branch> branches;
    private List<String> learningPath;
    private List<CommonMistake> commonMistakes;
    private List<Tip> tips;
    private List<String> relatedTopics;
    private SkillLevels skillLevels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Branch {
        private String title;
        private String description;
        private String nodeType;
        private String color;
        private String difficulty;
        private String cognitiveLevel;
        private List<SubBranch> subBranches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubBranch {
        private String title;
        private String content;
        private String nodeType;
        private String difficulty;
        private String cognitiveLevel;
        private List<String> keyPoints;
        private List<String> formulas;
        private List<Example> examples;
        private List<Exercise> exercises;
        private Concept concept;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String question;
        private String solution;
        private String difficulty;
        private String cognitiveLevel;
        private String estimatedTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exercise {
        private String question;
        private String answer;
        private String difficulty;
        private String cognitiveLevel;
        private List<String> hints;
        private String estimatedTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Concept {
        private String name;
        private String definition;
        private String explanation;
        private List<String> keyPoints;
        private List<String> examples;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommonMistake {
        private String mistake;
        private String reason;
        private String correction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tip {
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillLevels {
        private String beginner;
        private String intermediate;
        private String advanced;
    }
}
