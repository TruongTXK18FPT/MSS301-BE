package com.mss301.documentservice.service.integration;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface EmbeddingService {
    List<Float> generateEmbedding(String text);
}
