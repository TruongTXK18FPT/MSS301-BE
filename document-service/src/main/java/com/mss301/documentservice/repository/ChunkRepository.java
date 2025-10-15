package com.mss301.documentservice.repository;

import com.mss301.documentservice.entity.Chunk;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends ElasticsearchRepository<Chunk, String> {

    List<Chunk> findByDocumentIdOrderByChunkIndex(String documentId);

    List<Chunk> findByDocumentIdAndStructure_PageNumber(String documentId, Integer pageNumber);

    List<Chunk> findByStructure_ChapterId(String chapterId);

    List<Chunk> findByDocumentIdAndStructure_ChapterNumber(String documentId, Integer chapterNumber);

    List<Chunk> findByDocumentIdAndStructure_ChapterNumberAndStructure_LessonNumber(
            String documentId, Integer chapterNumber, Integer lessonNumber);

    List<Chunk> findByDocumentIdAndStructure_LessonId(String documentId, String lessonId);

    List<Chunk> findByDocumentIdAndStructure_ChapterNumberOrderByChunkIndex(
            String documentId, Integer chapterNumber);

    List<Chunk> findByDocumentIdAndStructure_LessonIdOrderByChunkIndex(
            String documentId, String lessonId);

    void deleteByDocumentId(String documentId);

}
