package com.mss301.retrievalservice.service;

import com.mss301.retrievalservice.dto.request.RetrievalRequest;
import com.mss301.retrievalservice.dto.response.RetrievalResult;

import java.io.IOException;
import java.util.List;

public interface RetrievalService {
    List<RetrievalResult> query(RetrievalRequest request) throws IOException;
}
