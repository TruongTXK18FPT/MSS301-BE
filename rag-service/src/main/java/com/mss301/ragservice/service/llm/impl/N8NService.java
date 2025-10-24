package com.mss301.ragservice.service.llm.impl;

import org.springframework.stereotype.Service;

import com.mss301.ragservice.enums.ResponseMode;
import com.mss301.ragservice.service.llm.LLMService;

@Service
public class N8NService implements LLMService {
    @Override
    public String generateResponse(String query, String context, ResponseMode mode) {
        return "";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
