package com.mss301.ragservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapResponse {
    private String mindmapContent;
    private List<Reference> references;
    private int totalReferences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reference {
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