package com.mss301.ragservice.service;

import org.springframework.stereotype.Service;

import com.mss301.ragservice.dto.request.TtsRequest;
import com.mss301.ragservice.dto.response.TtsResponse;

@Service
public interface TtsService {
    TtsResponse speak(TtsRequest request);
}
