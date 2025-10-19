package com.mss301.documentservice.service.document;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

public interface DocumentProcessingService {
    CompletableFuture<Void> processDocumentAsync(String documentId, String jobId);

    int estimateTokenCount(String text);

    void handleProcessingError(String documentId, String jobId, Exception error);
}
