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
public class DocumentListDto {
    private List<DocumentResponseDto> documents;
    private int total;

    public static DocumentListDto of(List<DocumentResponseDto> documents) {
        return DocumentListDto.builder()
                .documents(documents)
                .total(documents.size())
                .build();
    }
}
