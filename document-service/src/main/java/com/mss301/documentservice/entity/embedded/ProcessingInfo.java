package com.mss301.documentservice.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingInfo {
    private Integer tokenCount;
    private String language;

    // In4 cho OCR feat:
    private Boolean fromOcr;
    private Double ocrConfidence;
}
