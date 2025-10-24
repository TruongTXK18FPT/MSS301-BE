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
public class ChatResponse {
    private String answer;
    private List<Source> sources;
    private int totalSources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
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
