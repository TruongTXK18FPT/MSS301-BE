package com.mss301.documentservice.dto.response;

import java.util.List;

import com.mss301.documentservice.entity.Chunk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkSearchDto {
    private String query;
    private List<ChunkDto> matches;
    private int totalMatches;

    public static ChunkSearchDto of(String query, List<Chunk> chunks) {
        List<ChunkDto> chunkDtos = chunks.stream().map(ChunkDto::fromEntity).toList();

        return ChunkSearchDto.builder()
                .query(query)
                .matches(chunkDtos)
                .totalMatches(chunkDtos.size())
                .build();
    }
}
