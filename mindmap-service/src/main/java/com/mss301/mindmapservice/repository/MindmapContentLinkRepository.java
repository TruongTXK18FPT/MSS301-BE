package com.mss301.mindmapservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.MindmapContentLink;

@Repository
public interface MindmapContentLinkRepository extends JpaRepository<MindmapContentLink, Long> {
    List<MindmapContentLink> findByMindmapId(Long mindmapId);

    Optional<MindmapContentLink> findByMindmapIdAndContentId(Long mindmapId, Long contentId);
}
