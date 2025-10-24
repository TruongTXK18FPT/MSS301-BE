package com.mss301.documentservice.service.analysis.models.structure;

import lombok.Data;

@Data
public class StructureContext {
    private ChapterInfo chapter;
    private LessonInfo lesson;

    public boolean hasChapter() {
        return chapter != null;
    }

    public boolean hasLesson() {
        return lesson != null;
    }
}
