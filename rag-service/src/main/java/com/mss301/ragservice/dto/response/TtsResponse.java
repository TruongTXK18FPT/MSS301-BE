package com.mss301.ragservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {
    private boolean success;
    private String message;
    private String audioUrl;
}
