package com.mss301.documentservice.service.chunk;

import java.util.List;
import java.util.Optional;

import com.mss301.documentservice.entity.Chunk;

public interface ChunkingService {
    List<Chunk> createStructuredChunks(
            String documentId, String fullText, int maxChunkSize, int overlap, String language, int totalPages);

    List<Chunk> findByDocumentIdOrderByChunkIndex(String documentId);

    List<Chunk> findByDocumentIdAndStructure_ChapterNumber(String documentId, Integer chapterNumber);

    List<Chunk> findByDocumentIdAndStructure_ChapterNumberAndStructure_LessonNumber(
            String documentId, Integer chapterNumber, Integer lessonNumber);

    Optional<Chunk> findById(String chunkId);
}
