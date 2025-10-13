package com.mss301.mindmapservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.MindmapEdge;

@Repository
public interface MindmapEdgeRepository extends JpaRepository<MindmapEdge, Long> {

    List<MindmapEdge> findByMindmapId(Long mindmapId);

    List<MindmapEdge> findByFromNodeId(Long fromNodeId);

    List<MindmapEdge> findByToNodeId(Long toNodeId);

    List<MindmapEdge> findByFromNodeIdAndToNodeId(Long fromNodeId, Long toNodeId);

    @Query(
            "SELECT e FROM MindmapEdge e WHERE e.mindmapId = :mindmapId AND (e.fromNodeId = :nodeId OR e.toNodeId = :nodeId)")
    List<MindmapEdge> findByMindmapIdAndNodeId(@Param("mindmapId") Long mindmapId, @Param("nodeId") Long nodeId);

    @Query("SELECT COUNT(e) FROM MindmapEdge e WHERE e.mindmapId = :mindmapId")
    Long countByMindmapId(@Param("mindmapId") Long mindmapId);

    @Query("SELECT e FROM MindmapEdge e WHERE e.mindmapId = :mindmapId AND e.relationshipType = :relationshipType")
    List<MindmapEdge> findByMindmapIdAndRelationshipType(
            @Param("mindmapId") Long mindmapId,
            @Param("relationshipType") MindmapEdge.RelationshipType relationshipType);

    @Query("SELECT e FROM MindmapEdge e WHERE e.mindmapId = :mindmapId AND e.isDirected = :isDirected")
    List<MindmapEdge> findByMindmapIdAndIsDirected(
            @Param("mindmapId") Long mindmapId, @Param("isDirected") Boolean isDirected);

    Optional<MindmapEdge> findByIdAndMindmapId(Long id, Long mindmapId);

    boolean existsByIdAndMindmapId(Long id, Long mindmapId);

    boolean existsByFromNodeIdAndToNodeId(Long fromNodeId, Long toNodeId);
}
