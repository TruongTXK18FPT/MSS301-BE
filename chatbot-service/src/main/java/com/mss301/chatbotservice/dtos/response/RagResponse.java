package com.mss301.chatbotservice.dtos.response;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.enums.ResponseMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

