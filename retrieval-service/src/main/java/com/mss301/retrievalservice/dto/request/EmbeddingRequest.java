package com.mss301.retrievalservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingRequest {
    private String model;
    private String input;
}
