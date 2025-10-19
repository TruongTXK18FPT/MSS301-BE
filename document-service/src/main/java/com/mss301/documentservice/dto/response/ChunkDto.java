package com.mss301.documentservice.dto.response;

import com.mss301.documentservice.entity.Chunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDto {
    private String id;
    private Integer chunkIndex;
    private String content;
    private Integer pageNumber;
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer lessonNumber;
    private String lessonTitle;
    private String lessonId;
    private Integer tokenCount;
    private Boolean fromOcr;
    private Double confidence;
    private List<Float> embedding;

    public static ChunkDto fromEntity(Chunk chunk) {
        ChunkDtoBuilder builder = ChunkDto.builder()
                .id(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .embedding(chunk.getEmbedding());

        if (chunk.getStructure() != null) {
            builder.pageNumber(chunk.getStructure().getPageNumber())
                    .chapterNumber(chunk.getStructure().getChapterNumber())
                    .chapterTitle(chunk.getStructure().getChapterTitle())
                    .lessonNumber(chunk.getStructure().getLessonNumber())
                    .lessonTitle(chunk.getStructure().getLessonTitle())
                    .lessonId(chunk.getStructure().getLessonId());
        }

        if (chunk.getProcessingInfo() != null) {
            builder.tokenCount(chunk.getProcessingInfo().getTokenCount())
                    .fromOcr(chunk.getProcessingInfo().getFromOcr())
                    .confidence(chunk.getProcessingInfo().getOcrConfidence());
        }

        return builder.build();
    }
}
