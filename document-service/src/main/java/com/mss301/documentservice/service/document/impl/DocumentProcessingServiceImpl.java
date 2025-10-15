package com.mss301.documentservice.service.document.impl;

import com.mss301.documentservice.entity.Chunk;
import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.embedded.DocumentStructure;
import com.mss301.documentservice.entity.embedded.ProcessingInfo;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import com.mss301.documentservice.entity.enums.JobStatus;
import com.mss301.documentservice.repository.ChunkRepository;
import com.mss301.documentservice.repository.DocumentRepository;
import com.mss301.documentservice.repository.ProcessingJobRepository;
import com.mss301.documentservice.service.chunk.ChunkingService;
import com.mss301.documentservice.service.document.DocumentProcessingService;
import com.mss301.documentservice.service.extraction.PdfExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final ChunkRepository chunkRepository;
    private final PdfExtractorService pdfExtractorService; // For OCR fallback
    private final ChunkingService chunkingService;

    private static final int MIN_CHARS_PER_PAGE = 20; // Threshold for digital-born PDF detection
    private static final int CHUNK_SIZE = 1500; // Increase chunk size
    private static final int CHUNK_OVERLAP = 100;

    //Chạy background thread
    @Override
    @Async("documentProcessingExecutor")
    public CompletableFuture<Void> processDocumentAsync(String documentId, String jobId) {

        long startTime = System.currentTimeMillis();
        try{
            updateJobStatus(jobId, JobStatus.RUNNING, 5, "Starting document processing");

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

            File pdfFile = new File(document.getFilePath());
            if (!pdfFile.exists()) {
                throw new IllegalArgumentException("PDF file not found: " + document.getFilePath());
            }

            updateJobStatus(jobId, JobStatus.RUNNING, 5, "Document processing");

            List<String> pageTexts = extractTextDirectly(pdfFile);
            boolean usedOcr = false;

            updateJobStatus(jobId, JobStatus.RUNNING, 30, "Document processing");

            if (needsOcrFallback(pageTexts)) {
                log.info("OCR FALLBACK PASSED");

                pageTexts = extractTextWithOcr(pdfFile);
                usedOcr = true;
                updateJobStatus(jobId, JobStatus.RUNNING, 60, "OCR processing");
            } else {
                log.info("OCR FALLBACK NOT NEEDED");
                updateJobStatus(jobId, JobStatus.RUNNING, 60, "Document processing");
            }

            String fullText = String.join("\n", pageTexts);
            updateJobStatus(jobId, JobStatus.RUNNING, 70, "Create text chunks");

            List<Chunk> chunks = createChunks(document, pageTexts, fullText, usedOcr);

            updateJobStatus(jobId, JobStatus.RUNNING, 85, "Saving chunks to database");

            if (chunks.isEmpty()) {
                log.warn("No chunks found for document {}", documentId);
            } else {
                chunkRepository.saveAll(chunks);
            }

            document.setStatus(DocumentStatus.COMPLETED);
            document.setProcessedAt(LocalDateTime.now());
            document.setTotalPages(pageTexts.size());
            documentRepository.save(document);

            long processingTime = System.currentTimeMillis() - startTime;

            ProcessingJob job = processingJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus(JobStatus.COMPLETED);
                job.setProgress(100);
                job.setCompletedAt(LocalDateTime.now());
                job.setCurrentStep("Processing completed successfully");
                job.setTotalPages(pageTexts.size());
                job.setProcessedPages(pageTexts.size());
                job.setUsedOcr(usedOcr);
                job.setProcessingTimeMs(processingTime);
                processingJobRepository.save(job);
            }

            log.info("Document processing completed successfully: {} ({}ms, OCR: {})",
                    documentId, processingTime, usedOcr);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("Document processing failed after {} ms", (endTime - startTime), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<String> extractTextDirectly(File pdfFile) throws IOException {
        List<String> pageTexts = new ArrayList<>();

        try(PDDocument document = PDDocument.load(pdfFile)){
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i=1; i<=totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document).trim();
                pageTexts.add(text != null? text.trim(): "");
            }

        }
        return pageTexts;
    }

    public boolean needsOcrFallback(List<String> pageTexts) {
        int totalChars = 0;
        int pagesWithText = 0;

        for (String pageText :pageTexts) {
            int pageChars = pageText.length();
            totalChars += pageChars;

            if (pageChars >= MIN_CHARS_PER_PAGE) {
                pagesWithText++;
            }
        }

        double contentRatio = (double) pagesWithText / pageTexts.size();
        boolean needsOcr = contentRatio < 0.5;

        log.info("Document analysis: totalChars={}, pagesWithText={}, totalPages={}, contentRatio={}",
                totalChars, pagesWithText, pageTexts.size(), contentRatio);

        return needsOcr;
    }

    private List<String> extractTextWithOcr(File pdfFile) {
        return pdfExtractorService.extractPages(pdfFile);
    }

    private List<Chunk> createChunks(Document document, List<String> pageTexts, String fullText, boolean usedOcr) {
        List<Chunk> chunks = chunkingService.createStructuredChunks(
                document.getId(),
                fullText,
                CHUNK_SIZE,
                CHUNK_OVERLAP,
                document.getLanguage().toString(),
                pageTexts.size()
        );

        for (int i =0; i <chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            chunk.setId(UUID.randomUUID().toString());

            int sourcePage = findSourcePage(chunk.getContent(), pageTexts);

            if (chunk.getStructure() != null){
                DocumentStructure structure = chunk.getStructure();

                chunk.setStructure(
                  DocumentStructure.builder()
                          .pageNumber(sourcePage)
                          .chapterId(structure.getChapterId())
                          .chapterNumber(structure.getChapterNumber())
                          .chapterTitle(structure.getChapterTitle())
                          .lessonId(structure.getLessonId())
                          .lessonNumber(structure.getLessonNumber())
                          .lessonTitle(structure.getLessonTitle())
                          .build()
                );
            } else {
                chunk.setStructure(
                        DocumentStructure.builder()
                                .pageNumber(sourcePage)
                                .build()
                );
            }

            if (chunk.getProcessingInfo() != null){
                ProcessingInfo info = chunk.getProcessingInfo();

                chunk.setProcessingInfo(ProcessingInfo.builder()
                        .tokenCount(info.getTokenCount())
                        .language(info.getLanguage())
                        .fromOcr(usedOcr)
                        .ocrConfidence(usedOcr ? 0.85 : 0.99)
                        .build());
            } else {
                chunk.setProcessingInfo(ProcessingInfo.builder()
                        .tokenCount(estimateTokenCount(chunk.getContent()))
                        .language(document.getLanguage().toString())
                        .fromOcr(usedOcr)
                        .ocrConfidence(usedOcr ? 0.85 : 0.99)
                        .build());
            }
        }

        log.info("Saving {} chunks for document {}", chunks.size(), document.getId());
        return chunks;
    }

    private int findSourcePage(String chunkText, List<String> pageTexts) {
        int bestPage = 1;
        int maxOverlap = 0;

        for (int i=0; i<pageTexts.size(); i++) {
            String pageText = pageTexts.get(i);
            int overlap = calculateTextOverlap(chunkText, pageText);
            if (overlap > maxOverlap) {
                maxOverlap = overlap;
                bestPage = i + 1;
            }
        }

        return bestPage;
    }

    private int calculateTextOverlap(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        words1.retainAll(words2);
        return words1.size();
    }

    @Override
    public int estimateTokenCount(String text) {
        return 0;
    }

    private void updateJobStatus(String jobId, JobStatus status, int progress, String currentStep) {
        try {
            processingJobRepository.findById(jobId).ifPresent(processingJob -> {
                processingJob.setStatus(status);
                processingJob.setProgress(progress);
                processingJob.setCurrentStep(currentStep);
                processingJobRepository.save(processingJob);
            });
        } catch (Exception e) {
            log.error("Error while updating job status", e);
        }
    }

    @Override
    public void handleProcessingError(String documentId, String jobId, Exception error) {

    }
}
