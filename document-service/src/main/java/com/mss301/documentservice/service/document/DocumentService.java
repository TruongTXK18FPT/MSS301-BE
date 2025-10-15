package com.mss301.documentservice.service.document;

import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public interface DocumentService {
    Document uploadPdf(MultipartFile file, String title, String description) throws IOException;
    ProcessingJob triggerProcessing(String documentId);
    List<Document> getAllDocuments();
    List<Document> getDocumentsByStatus(DocumentStatus status);
    Optional<ProcessingJob> getProcessingStatus(String documentId);
    Optional<Document> getDocumentById(String documentId);
    void deleteDocument(String documentId);
}
