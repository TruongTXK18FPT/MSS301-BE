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
public class TocAnalysisDto {
    private String documentId;
    private int fullTextLength;
    private String message;
    private String textPreview;
    private List<Object> tocEntries;

    public static TocAnalysisDto of(String documentId, int fullTextLength, String textPreview) {
        return TocAnalysisDto.builder()
                .documentId(documentId)
                .fullTextLength(fullTextLength)
                .message("Table of contents analysis - check logs for detailed parsing results")
                .textPreview(textPreview)
                .tocEntries(List.of())
                .build();
    }
}
