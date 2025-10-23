package com.mss301.ragservice.strategy;

import com.mss301.ragservice.dto.external.RetrievalResponse;

import java.util.List;

public interface ResponseStrategy {

    Object formatResponse(String llmResponse, List<RetrievalResponse.RetrievalResult> results);
}
