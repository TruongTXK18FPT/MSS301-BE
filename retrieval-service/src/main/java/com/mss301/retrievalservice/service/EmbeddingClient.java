package com.mss301.retrievalservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mss301.retrievalservice.dto.request.EmbeddingRequest;
import com.mss301.retrievalservice.dto.response.EmbeddingResponse;

@FeignClient(name = "embedding-client", url = "${embedding.client.url}")
public interface EmbeddingClient {

    @PostMapping("/embeddings")
    EmbeddingResponse createEmbedding(@RequestBody EmbeddingRequest request);
}
