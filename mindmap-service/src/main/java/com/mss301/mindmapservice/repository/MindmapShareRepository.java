package com.mss301.mindmapservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.MindmapShare;

@Repository
public interface MindmapShareRepository extends JpaRepository<MindmapShare, Long> {

    List<MindmapShare> findByMindmapId(Long mindmapId);

    List<MindmapShare> findBySharedByUserId(Long sharedByUserId);

    List<MindmapShare> findBySharedWithUserId(Long sharedWithUserId);

    List<MindmapShare> findByShareCode(String shareCode);

    List<MindmapShare> findByIsActiveTrue();

    List<MindmapShare> findByMindmapIdAndIsActiveTrue(Long mindmapId);

    List<MindmapShare> findBySharedWithUserIdAndIsActiveTrue(Long sharedWithUserId);

    @Query(
            "SELECT s FROM MindmapShare s WHERE s.mindmapId = :mindmapId AND s.isActive = true AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<MindmapShare> findActiveSharesByMindmapId(@Param("mindmapId") Long mindmapId, @Param("now") LocalDateTime now);

    @Query(
            "SELECT s FROM MindmapShare s WHERE s.sharedWithUserId = :userId AND s.isActive = true AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<MindmapShare> findActiveSharesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query(
            "SELECT s FROM MindmapShare s WHERE s.shareCode = :shareCode AND s.isActive = true AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    Optional<MindmapShare> findActiveShareByCode(@Param("shareCode") String shareCode, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM MindmapShare s WHERE s.mindmapId = :mindmapId AND s.isActive = true")
    Long countActiveSharesByMindmapId(@Param("mindmapId") Long mindmapId);

    Optional<MindmapShare> findByIdAndMindmapId(Long id, Long mindmapId);

    boolean existsByIdAndMindmapId(Long id, Long mindmapId);

    boolean existsByShareCode(String shareCode);
}
