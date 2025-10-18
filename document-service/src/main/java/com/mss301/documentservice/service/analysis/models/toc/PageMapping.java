package com.mss301.documentservice.service.analysis.models.toc;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageMapping {
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer lessonNumber;
    private String lessonTitle;
    private String lessonId;
}