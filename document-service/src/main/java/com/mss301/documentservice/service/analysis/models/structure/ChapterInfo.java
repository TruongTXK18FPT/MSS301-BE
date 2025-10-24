package com.mss301.documentservice.service.analysis.models.structure;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ChapterInfo {
    private int number;
    private String title;
    private int startPosition;
    private int endPosition;
    private List<LessonInfo> lessons = new ArrayList<>();
}
