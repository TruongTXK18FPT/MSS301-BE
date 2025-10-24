package com.mss301.ragservice.strategy;

import java.util.List;

import com.mss301.ragservice.dto.external.RetrievalResponse;

public interface ResponseStrategy {

    Object formatResponse(String llmResponse, List<RetrievalResponse.RetrievalResult> results);
}
