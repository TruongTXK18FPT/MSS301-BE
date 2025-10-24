package com.mss301.ragservice.dto.external;

import java.util.List;

import lombok.Data;

@Data
public class RetrievalResponse {
    private List<RetrievalResult> results;
    private String documentId;
    private String chapterId;
    private String lessonId;
    private int totalResults;

    @Data
    public static class RetrievalResult {
        private String content;
        private double score;
        private String documentId;
        private String chapterId;
        private String lessonId;
        private String chapterTitle;
        private String lessonTitle;
        private Long pageNumber;
    }
}
