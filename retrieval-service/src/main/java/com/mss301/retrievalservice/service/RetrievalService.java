package com.mss301.retrievalservice.service;

import java.io.IOException;
import java.util.List;

import com.mss301.retrievalservice.dto.request.RetrievalRequest;
import com.mss301.retrievalservice.dto.response.RetrievalResult;

public interface RetrievalService {
    List<RetrievalResult> query(RetrievalRequest request) throws IOException;
}
