package com.mss301.documentservice.service.document;

import com.mss301.documentservice.entity.Chunk;
import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.enums.JobStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public interface DocumentProcessingService {
    CompletableFuture<Void> processDocumentAsync(String documentId, String jobId);
    int estimateTokenCount(String text);
    void handleProcessingError(String documentId, String jobId, Exception error);
}
