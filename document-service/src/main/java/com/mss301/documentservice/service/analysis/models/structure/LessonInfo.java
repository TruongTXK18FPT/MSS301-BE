package com.mss301.documentservice.service.analysis.models.structure;

import lombok.Data;

@Data
public class LessonInfo {
    private int number;
    private String title;
    private int startPosition;
    private int endPosition;
    private int chapterNumber;
    private String lessonId;
}
