package com.mss301.documentservice.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStructure {
    private Integer pageNumber;

    // Chapter:
    private String chapterId;
    private Integer chapterNumber;
    private String chapterTitle;

    // Lesson:
    private String lessonId;
    private Integer lessonNumber;
    private String lessonTitle;
}
