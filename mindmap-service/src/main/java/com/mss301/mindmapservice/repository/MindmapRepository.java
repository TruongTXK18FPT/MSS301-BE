package com.mss301.mindmapservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.Mindmap;

@Repository
public interface MindmapRepository extends JpaRepository<Mindmap, Long> {

    List<Mindmap> findByUserId(Long userId);

    Page<Mindmap> findByUserId(Long userId, Pageable pageable);

    List<Mindmap> findByUserIdAndIsPublic(Long userId, Boolean isPublic);

    List<Mindmap> findByIsPublicTrue();

    Page<Mindmap> findByIsPublicTrue(Pageable pageable);

    List<Mindmap> findByGradeAndSubject(String grade, String subject);

    List<Mindmap> findByGradeAndSubjectAndIsPublic(String grade, String subject, Boolean isPublic);

    List<Mindmap> findByIsAiGeneratedTrue();

    List<Mindmap> findByAiProvider(String aiProvider);

    @Query(
            "SELECT m FROM Mindmap m WHERE m.userId = :userId AND (m.title LIKE %:keyword% OR m.description LIKE %:keyword%)")
    List<Mindmap> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query(
            "SELECT m FROM Mindmap m WHERE m.isPublic = true AND (m.title LIKE %:keyword% OR m.description LIKE %:keyword%)")
    List<Mindmap> findByPublicAndKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(m) FROM Mindmap m WHERE m.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Mindmap m WHERE m.userId = :userId ORDER BY m.lastAccessedAt DESC")
    List<Mindmap> findByUserIdOrderByLastAccessedDesc(@Param("userId") Long userId);

    @Query("SELECT m FROM Mindmap m WHERE m.userId = :userId ORDER BY m.createdAt DESC")
    List<Mindmap> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT m FROM Mindmap m WHERE m.isPublic = true ORDER BY m.favoriteCount DESC")
    List<Mindmap> findPublicOrderByFavoriteCountDesc();

    @Query("SELECT m FROM Mindmap m WHERE m.isPublic = true ORDER BY m.accessCount DESC")
    List<Mindmap> findPublicOrderByAccessCountDesc();

    Optional<Mindmap> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
