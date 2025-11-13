package com.mss301.documentservice.service.document;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.mss301.documentservice.dto.google.FileSearchStoreResponse;
import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.DocumentStatus;

public interface DocumentService {
    Document uploadPdf(MultipartFile file, String title, String description) throws IOException;

    ProcessingJob triggerProcessing(String documentId);

    List<Document> getAllDocuments();

    List<Document> getDocumentsByStatus(DocumentStatus status);

    Optional<ProcessingJob> getProcessingStatus(String documentId);

    Optional<Document> getDocumentById(String documentId);

    void deleteDocument(String documentId);

    // Google File Search Store operations
    List<FileSearchStoreResponse> listGoogleFileSearchStores();

    FileSearchStoreResponse getGoogleFileSearchStore(String storeName);
}
