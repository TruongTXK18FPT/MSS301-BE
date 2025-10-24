package com.mss301.documentservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mss301.documentservice.config.EmbeddingFeignConfig;
import com.mss301.documentservice.dto.request.EmbeddingRequest;
import com.mss301.documentservice.dto.response.EmbeddingResponse;

@FeignClient(name = "embedding-client", url = "${embedding.client.url}", configuration = EmbeddingFeignConfig.class)
public interface EmbeddingClient {

    @PostMapping("/embeddings")
    EmbeddingResponse createEmbedding(@RequestBody EmbeddingRequest request);
}
