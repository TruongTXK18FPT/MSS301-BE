package com.mss301.documentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static PaginationDto of(int page, int size, long totalElements) {
        return PaginationDto.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages((int) ((totalElements + size - 1) / size))
                .build();
    }
}
