package com.mss301.documentservice.service.integration;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface EmbeddingService {
    List<Float> generateEmbedding(String text);
}
