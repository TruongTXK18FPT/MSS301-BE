package com.mss301.mindmapservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.MindmapNode;

@Repository
public interface MindmapNodeRepository extends JpaRepository<MindmapNode, Long> {

    List<MindmapNode> findByMindmapId(Long mindmapId);

    List<MindmapNode> findByMindmapIdOrderByOrderIndex(Long mindmapId);

    List<MindmapNode> findByMindmapIdAndParentNodeId(Long mindmapId, Long parentNodeId);

    List<MindmapNode> findByMindmapIdAndParentNodeIdIsNull(Long mindmapId);

    List<MindmapNode> findByMindmapIdAndLevel(Long mindmapId, Integer level);

    List<MindmapNode> findByParentNodeId(Long parentNodeId);

    @Query("SELECT n FROM MindmapNode n WHERE n.mindmapId = :mindmapId AND n.nodeType = :nodeType")
    List<MindmapNode> findByMindmapIdAndNodeType(
            @Param("mindmapId") Long mindmapId, @Param("nodeType") MindmapNode.NodeType nodeType);

    @Query(
            "SELECT n FROM MindmapNode n WHERE n.mindmapId = :mindmapId AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)")
    List<MindmapNode> findByMindmapIdAndKeyword(@Param("mindmapId") Long mindmapId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(n) FROM MindmapNode n WHERE n.mindmapId = :mindmapId")
    Long countByMindmapId(@Param("mindmapId") Long mindmapId);

    @Query(
            "SELECT MAX(n.orderIndex) FROM MindmapNode n WHERE n.mindmapId = :mindmapId AND n.parentNodeId = :parentNodeId")
    Optional<Integer> findMaxOrderIndexByMindmapIdAndParentNodeId(
            @Param("mindmapId") Long mindmapId, @Param("parentNodeId") Long parentNodeId);

    @Query("SELECT n FROM MindmapNode n WHERE n.mindmapId = :mindmapId ORDER BY n.level, n.orderIndex")
    List<MindmapNode> findByMindmapIdOrderByLevelAndOrderIndex(@Param("mindmapId") Long mindmapId);

    Optional<MindmapNode> findByIdAndMindmapId(Long id, Long mindmapId);

    boolean existsByIdAndMindmapId(Long id, Long mindmapId);
}
