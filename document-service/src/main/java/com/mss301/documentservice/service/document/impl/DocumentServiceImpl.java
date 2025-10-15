package com.mss301.documentservice.service.document.impl;

import ch.qos.logback.core.model.processor.ProcessingPhase;
import co.elastic.clients.elasticsearch.ingest.Local;
import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import com.mss301.documentservice.entity.enums.JobStatus;
import com.mss301.documentservice.entity.enums.Language;
import com.mss301.documentservice.repository.ChunkRepository;
import com.mss301.documentservice.repository.DocumentRepository;
import com.mss301.documentservice.repository.ProcessingJobRepository;
import com.mss301.documentservice.service.document.DocumentProcessingService;
import com.mss301.documentservice.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentProcessingService documentProcessingService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public Document uploadPdf(MultipartFile file, String title, String description) throws IOException {
        // Kiểm tra file có hợp lệ không?
        validateFile(file);

        // Tạo thư mục upload nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        if (!uploadPath.toFile().exists()) {
            uploadPath.toFile().mkdirs();
        }

        // Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = java.util.UUID.randomUUID().toString() + fileExtension;

        // Lưu file vào thư mục uploads
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        log.info("File uploaded successfully");

        return documentRepository.save(
                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .title(title != null ? title : originalFilename)
                        .fileName(originalFilename)
                        .filePath(filePath.toString())
                        .status(DocumentStatus.UPLOADED)
                        .uploadedAt(LocalDateTime.now())
                        .size(file.getSize())
                        .contentType(file.getContentType())
                        .description(description)
                        .language(Language.VI)
                        .build()
        );
    }

    @Override
    public ProcessingJob triggerProcessing(String documentId) {

        //Check document có hợp lệ không
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (document.getStatus() != DocumentStatus.UPLOADED) {
            throw new IllegalStateException("Document is not in UPLOADED status: " + documentId);
        }

        Optional<ProcessingJob> existingJob = processingJobRepository.findByDocumentId(documentId);
        if (existingJob.isPresent() && existingJob.get().getStatus() == JobStatus.RUNNING) {
            throw new IllegalStateException("Processing job is already in progress for document: " + documentId);
        }

        ProcessingJob savedJob = processingJobRepository.save(
                ProcessingJob.builder()
                        .id(UUID.randomUUID().toString())
                        .documentId(documentId)
                        .status(JobStatus.PENDING)
                        .currentStep("STARTING")
                        .progress(0)
                        .startedAt(LocalDateTime.now())
                        .build()
        );

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        documentProcessingService.processDocumentAsync(documentId, savedJob.getId());
        log.info("Document {} has been processed", documentId);
        return savedJob;
    }

    @Override
    public List<Document> getAllDocuments() {
        return List.of();
    }

    @Override
    public List<Document> getDocumentsByStatus(DocumentStatus status) {
        return List.of();
    }

    @Override
    public Optional<ProcessingJob> getProcessingStatus(String documentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Document> getDocumentById(String documentId) {
        return Optional.empty();
    }

    @Override
    public void deleteDocument(String documentId) {

    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!"appication/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
        long maxFileSize = 50 * 1024 * 1024; // 50 MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 50MB");
        }
    }
}
