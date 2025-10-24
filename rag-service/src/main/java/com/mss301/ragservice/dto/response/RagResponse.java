package com.mss301.ragservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.enums.ResponseMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {
    private ResponseMode mode;
    private LLMProvider llmProvider;
    private String queryText;
    private Object content;
    private LocalDateTime timestamp;
    private int chunksUsed;
}
