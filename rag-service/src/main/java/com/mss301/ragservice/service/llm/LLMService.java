package com.mss301.ragservice.service.llm;

import com.mss301.ragservice.enums.ResponseMode;

public interface LLMService {
    String generateResponse(String query, String context, ResponseMode mode);

    boolean isAvailable();
}
