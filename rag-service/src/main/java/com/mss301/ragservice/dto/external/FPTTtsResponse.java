package com.mss301.ragservice.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FPTTtsResponse {
    private String async;
    private int error;
    private String message;

    @JsonProperty("request_id")
    private String requestId;
}
