package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.dtos.request.RagRequest;
import com.mss301.chatbotservice.dtos.response.RagResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rag-service-client", url = "${service.rag.url}")
public interface RagService {
    @PostMapping("/api/v1/rag/query")
    RagResponse processQuery(@RequestBody RagRequest request);
}
