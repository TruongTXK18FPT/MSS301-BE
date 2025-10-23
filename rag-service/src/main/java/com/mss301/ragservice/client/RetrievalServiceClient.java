package com.mss301.ragservice.client;

import com.mss301.ragservice.config.FeignConfig;
import com.mss301.ragservice.dto.external.RetrievalRequest;
import com.mss301.ragservice.dto.external.RetrievalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "retrieval-service",
        url = "${external.retrieval-service.url}",
        configuration = FeignConfig.class
)
@Service
public interface RetrievalServiceClient {

    @PostMapping("/api/v1/retrieval/query")
    List<RetrievalResponse> searchChunks(@RequestBody RetrievalRequest request);
}
