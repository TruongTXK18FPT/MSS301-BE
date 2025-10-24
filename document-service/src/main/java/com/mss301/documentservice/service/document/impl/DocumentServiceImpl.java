package com.mss301.documentservice.service.document.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Xác định đường dẫn thư mục uploads luôn nằm trong document-service
     * Bất kể ứng dụng chạy từ đâu
     */
    private Path resolveUploadPath() {
        // Lấy thư mục làm việc hiện tại
        String currentDir = System.getProperty("user.dir");
        File currentDirFile = new File(currentDir);

        log.debug("Current working directory: {}", currentDir);

        Path uploadPath;

        // Kiểm tra xem đang chạy từ document-service hay không
        if (currentDirFile.getName().equals("document-service")) {
            // Đang chạy từ trong document-service -> sử dụng trực tiếp
            uploadPath = Paths.get(currentDir, "uploads");
            log.debug("Running from document-service, upload path: {}", uploadPath);
        } else {
            // Đang chạy từ root hoặc nơi khác
            // Kiểm tra xem document-service có tồn tại trong thư mục hiện tại không
            File documentServiceDir = new File(currentDir, "document-service");
            if (documentServiceDir.exists() && documentServiceDir.isDirectory()) {
                // document-service tồn tại -> sử dụng nó
                uploadPath = Paths.get(currentDir, "document-service", "uploads");
                log.debug("Found document-service folder, upload path: {}", uploadPath);
            } else {
                // Không tìm thấy document-service, fallback về đường dẫn tương đối
                uploadPath = Paths.get(uploadDir);
                log.warn("document-service folder not found, using configured path: {}", uploadPath);
            }
        }

        return uploadPath;
    }

    @Override
    public Document uploadPdf(MultipartFile file, String title, String description) throws IOException {
        // Kiểm tra file có hợp lệ không?
        validateFile(file);

        // Xác định thư mục upload
        Path uploadPath = resolveUploadPath();

        // Tạo thư mục upload nếu chưa tồn tại
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory at: {}", uploadPath.toAbsolutePath());
        }

        log.info("Upload directory: {}", uploadPath.toAbsolutePath());

        // Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Original filename is empty");
        }

        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }

        String uniqueFilename = UUID.randomUUID() + fileExtension;

        // Lưu file vào thư mục uploads
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        log.info("File uploaded successfully to: {}", filePath.toAbsolutePath());

        return documentRepository.save(Document.builder()
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
                .build());
    }

    @Override
    public ProcessingJob triggerProcessing(String documentId) {

        // Check document có hợp lệ không
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (document.getStatus() != DocumentStatus.UPLOADED) {
            throw new IllegalStateException("Document is not in UPLOADED status: " + documentId);
        }

        Optional<ProcessingJob> existingJob = processingJobRepository.findByDocumentId(documentId);
        if (existingJob.isPresent() && existingJob.get().getStatus() == JobStatus.RUNNING) {
            throw new IllegalStateException("Processing job is already in progress for document: " + documentId);
        }

        ProcessingJob savedJob = processingJobRepository.save(ProcessingJob.builder()
                .id(UUID.randomUUID().toString())
                .documentId(documentId)
                .status(JobStatus.PENDING)
                .currentStep("STARTING")
                .progress(0)
                .startedAt(LocalDateTime.now())
                .build());

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        documentProcessingService.processDocumentAsync(documentId, savedJob.getId());
        log.info("Document {} has been processed", documentId);
        return savedJob;
    }

    @Override
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    @Override
    public List<Document> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatusOrderByUploadedAtDesc(status);
    }

    @Override
    public Optional<ProcessingJob> getProcessingStatus(String documentId) {
        return processingJobRepository.findByDocumentId(documentId);
    }

    @Override
    public Optional<Document> getDocumentById(String documentId) {
        return documentRepository.findById(documentId);
    }

    @Override
    public void deleteDocument(String documentId) {
        // Xóa tất cả chunks liên quan
        chunkRepository.deleteByDocumentId(documentId);

        // Xóa processing job nếu có
        processingJobRepository
                .findByDocumentId(documentId)
                .ifPresent(job -> processingJobRepository.deleteById(job.getId()));

        // Xóa document
        documentRepository.deleteById(documentId);

        log.info("Deleted document {} and all related data", documentId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
        long maxFileSize = 50 * 1024 * 1024; // 50 MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 50MB");
        }
    }
}
