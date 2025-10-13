package com.mss301.mindmapservice.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<GeminiContent> contents;
    private GeminiGenerationConfig generationConfig;
    private List<String> safetySettings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiContent {
        private List<GeminiPart> parts;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiPart {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiGenerationConfig {
        private Integer maxOutputTokens;
        private Double temperature;
        private Double topP;
        private Double topK;
    }
}
