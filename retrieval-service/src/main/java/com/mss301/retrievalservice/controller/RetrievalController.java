package com.mss301.retrievalservice.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.retrievalservice.dto.request.RetrievalRequest;
import com.mss301.retrievalservice.dto.response.RetrievalResult;
import com.mss301.retrievalservice.service.RetrievalService;

@RestController
@RequestMapping("api/v1/retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/query")
    public List<RetrievalResult> query(@RequestBody RetrievalRequest request) throws IOException {
        return retrievalService.query(request);
    }
}
