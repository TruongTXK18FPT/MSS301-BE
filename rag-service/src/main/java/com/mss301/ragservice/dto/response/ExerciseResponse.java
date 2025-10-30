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
public class ExerciseResponse {
    
    private String exercisesContent;
    private List<Reference> references;
    private Integer totalReferences;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reference {
        private String content;
        private Float score;
        private String documentId;
        private String chapterId;
        private String lessonId;
        private String chapterTitle;
        private String lessonTitle;
        private Integer pageNumber;
    }
}
