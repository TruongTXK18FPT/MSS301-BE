package com.mss301.retrievalservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetrievalResult {
    private String content;
    private double score;
    private String documentId;
    private String chapterId;
    private String lessonId;
    private String chapterTitle;
    private String lessonTitle;
    private Long pageNumber;
}
