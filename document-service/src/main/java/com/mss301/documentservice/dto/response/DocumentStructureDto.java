package com.mss301.documentservice.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStructureDto {
    private String documentId;
    private int totalChunks;
    private List<ChapterDto> structure;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterDto {
        private Integer number;
        private String title;
        private List<LessonDto> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDto {
        private Integer number;
        private String title;
        private String id;
        private Integer chunkCount;
    }
}
