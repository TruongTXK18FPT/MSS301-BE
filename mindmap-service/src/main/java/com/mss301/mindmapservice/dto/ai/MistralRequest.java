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
public class MistralRequest {

    private String model;
    private List<MistralMessage> messages;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Boolean stream;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistralMessage {
        private String role;
        private String content;
    }
}
