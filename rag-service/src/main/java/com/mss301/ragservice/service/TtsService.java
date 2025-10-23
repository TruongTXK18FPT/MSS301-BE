package com.mss301.ragservice.service;

import com.mss301.ragservice.dto.request.TtsRequest;
import com.mss301.ragservice.dto.response.TtsResponse;
import org.springframework.stereotype.Service;

@Service
public interface TtsService {
    TtsResponse speak(TtsRequest request);
}
