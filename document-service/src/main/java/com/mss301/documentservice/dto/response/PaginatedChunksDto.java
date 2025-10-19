package com.mss301.documentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedChunksDto {
    private List<ChunkDto> chunks;
    private PaginationDto pagination;

    public static PaginatedChunksDto of(List<ChunkDto> chunks, PaginationDto pagination) {
        return PaginatedChunksDto.builder()
                .chunks(chunks)
                .pagination(pagination)
                .build();
    }
}
