package com.mss301.documentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContentDto {
    private String content;
    private int totalChunks;
    private int totalCharacters;
    private boolean usedOcr;
    private String language;
    private ChunkStatsDto chunkStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkStatsDto {
        private double averageChunkSize;
        private int totalTokens;
    }
}
