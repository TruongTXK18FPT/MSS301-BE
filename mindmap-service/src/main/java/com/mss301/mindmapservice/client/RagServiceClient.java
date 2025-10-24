package com.mss301.mindmapservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.mss301.mindmapservice.dto.rag.RagRequest;
import com.mss301.mindmapservice.dto.rag.RagResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RagServiceClient {

    private final WebClient ragWebClient;

    public RagResponse processRagQuery(RagRequest request) {
        return ragWebClient
                .post()
                .uri("/api/v1/rag/query")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagResponse.class)
                .block();
    }
}
