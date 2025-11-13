package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.dtos.request.TtsRequest;
import com.mss301.chatbotservice.dtos.response.TtsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tts-service", url = "${service.rag.url}")
public interface TtsService {
    @PostMapping("/api/v1/tts/speak")
    TtsResponse speak(@RequestBody TtsRequest request);
}

