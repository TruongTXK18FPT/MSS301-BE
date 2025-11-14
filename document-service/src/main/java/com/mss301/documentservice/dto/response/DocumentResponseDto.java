package com.mss301.documentservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import com.mss301.documentservice.entity.enums.JobStatus;

import lombok.Data;

@Data
public class DocumentResponseDto {
    private String id;
    private String title;
    private String filename;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private Long size;
    private String language;
    private Integer totalPages;
    private String description;
    private String googleFileSearchStoreName; // Google File Search Store name (cho RAG)

    // Processing job info (if available)
    private ProcessingJobDto processingJob;

    @Data
    public static class ProcessingJobDto {
        private String id;
        private JobStatus status;
        private String currentStep;
        private Integer progress;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private Boolean usedOcr;
        private Long processingTimeMs;
    }

    public static DocumentResponseDto fromEntity(Document document) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setFilename(document.getFileName());
        dto.setStatus(document.getStatus());
        dto.setUploadedAt(document.getUploadedAt());
        dto.setProcessedAt(document.getProcessedAt());
        dto.setSize(document.getSize());
        dto.setLanguage(document.getLanguage().toString());
        dto.setTotalPages(document.getTotalPages());
        dto.setDescription(document.getDescription());
        dto.setGoogleFileSearchStoreName(document.getGoogleFileSearchStoreName());
        return dto;
    }

    public static ProcessingJobDto fromEntity(ProcessingJob job) {
        ProcessingJobDto dto = new ProcessingJobDto();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus());
        dto.setCurrentStep(job.getCurrentStep());
        dto.setProgress(job.getProgress());
        dto.setStartedAt(job.getStartedAt());
        dto.setCompletedAt(job.getCompletedAt());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setUsedOcr(job.getUsedOcr());
        dto.setProcessingTimeMs(job.getProcessingTimeMs());
        return dto;
    }
}
