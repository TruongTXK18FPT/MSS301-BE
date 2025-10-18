package com.mss301.documentservice.service.chunk.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.documentservice.entity.Chunk;
import com.mss301.documentservice.entity.embedded.DocumentStructure;
import com.mss301.documentservice.entity.embedded.ProcessingInfo;
import com.mss301.documentservice.service.analysis.DocumentStructureService;
import com.mss301.documentservice.service.analysis.models.structure.ChapterInfo;
import com.mss301.documentservice.service.analysis.models.structure.LessonInfo;
import com.mss301.documentservice.service.analysis.models.structure.StructureContext;
import com.mss301.documentservice.service.chunk.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private DocumentStructureService documentStructureService;

    private ChunkSplitter chunkSplitter;

    private ChunkDeduplicator chunkDeduplicator;

    private PageEstimator pageEstimator;

    private ChunkEmbedder chunkEmbedder;

    @Override
    public List<Chunk> createStructuredChunks(
            String documentId, String fullText, int maxChunkSize, int overlap, String language, int totalPages) {
        List<Chunk> chunks = new ArrayList<>();

        if (fullText == null || fullText.trim().isEmpty()) {
            return chunks;
        }

        // Analyze document structure with total pages for better TOC mapping
        com.mss301.documentservice.service.analysis.models.structure.DocumentStructure structure =
                documentStructureService.analyzeDocumentStructure(fullText, totalPages);

        // Initialize PageEstimator with TOC mapping for accurate page estimation
        @SuppressWarnings("unchecked")
        java.util.Map rawTocMap = (java.util.Map) documentStructureService.getTocPageMapping();
        pageEstimator.initializeWithTocMapping(rawTocMap, documentStructureService.getTotalPages());

        log.info(
                "ChunkingService: Retrieved TOC page mapping with {} entries, total pages: {}",
                documentStructureService.getTocPageMapping() != null
                        ? documentStructureService.getTocPageMapping().size()
                        : 0,
                documentStructureService.getTotalPages());

        // Clean up text
        String cleanText = chunkSplitter.cleanText(fullText);

        // Create basic chunks with deduplication
        List<String> textChunks = chunkDeduplicator.createChunksWithDeduplication(cleanText, maxChunkSize, overlap);

        // Map each chunk to its structure context
        int currentPosition = 0;
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);

            // Find the actual position of this chunk in the original text
            int chunkStartPosition = pageEstimator.findChunkPosition(cleanText, chunkText, currentPosition);
            int chunkEndPosition = chunkStartPosition + chunkText.length();

            // Estimate page number from position
            int estimatedPage = pageEstimator.estimatePageFromPosition(fullText, chunkStartPosition);

            // Find structure context for this chunk using page-based mapping with content
            // analysis fallback
            StructureContext context = documentStructureService.findStructureContextWithContentAnalysis(
                    structure, chunkStartPosition, estimatedPage, chunkText);

            // Debug log to track context finding with more detail (only for first 30 chunks
            // to avoid spam)
            if (i < 30) {
                log.info(
                        "Chunk {} at position {} (page {}): hasChapter={}, hasLesson={}",
                        i,
                        chunkStartPosition,
                        estimatedPage,
                        context.hasChapter(),
                        context.hasLesson());
                if (context.hasChapter()) {
                    log.info(
                            "  Chapter: {} - {}",
                            context.getChapter().getNumber(),
                            context.getChapter().getTitle());
                }
                if (context.hasLesson()) {
                    log.info(
                            "  Lesson: {} - {} (ID: {})",
                            context.getLesson().getNumber(),
                            context.getLesson().getTitle(),
                            context.getLesson().getLessonId());
                } else {
                    log.info("  No lesson found for chunk {} (page {})", i, estimatedPage);
                }
            }

            // Create chunk entity
            Chunk chunk = new Chunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunkText);

            // Generate embedding for the chunk content
            List<Float> embedding = chunkEmbedder.generateEmbedding(chunkText, i);
            chunk.setEmbedding(embedding);

            // ✨ Build DocumentStructure object
            DocumentStructure.DocumentStructureBuilder structureBuilder =
                    DocumentStructure.builder().pageNumber(estimatedPage);

            if (context.hasChapter()) {
                ChapterInfo chapter = context.getChapter();
                structureBuilder
                        .chapterId("chapter_" + chapter.getNumber())
                        .chapterNumber(chapter.getNumber())
                        .chapterTitle(chapter.getTitle());
            }

            if (context.hasLesson()) {
                LessonInfo lesson = context.getLesson();
                structureBuilder
                        .lessonId(lesson.getLessonId())
                        .lessonNumber(lesson.getNumber())
                        .lessonTitle(lesson.getTitle());
                log.debug(
                        "  Set lesson metadata: number={}, title={}, id={}",
                        lesson.getNumber(),
                        lesson.getTitle(),
                        lesson.getLessonId());
            } else {
                log.debug("  No lesson found for chunk {} (page {})", i, estimatedPage);
            }

            chunk.setStructure(structureBuilder.build());

            // ✨ Build ProcessingInfo object
            ProcessingInfo processingInfo = ProcessingInfo.builder()
                    .tokenCount(chunkEmbedder.estimateTokenCount(chunkText))
                    .language(language)
                    .fromOcr(false)
                    .ocrConfidence(null)
                    .build();

            chunk.setProcessingInfo(processingInfo);

            chunks.add(chunk);

            // Update position for next iteration
            currentPosition = chunkEndPosition - overlap;
        }

        log.info("Successfully created {} structured chunks with embeddings", chunks.size());
        return chunks;
    }
}
