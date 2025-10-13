package com.mss301.contentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findByOwnerId(Long ownerId);

    List<ContentItem> findByIsPublicTrue();

    List<ContentItem> findByOwnerIdAndType(Long ownerId, Type type);

    @Query(
            "SELECT c FROM ContentItem c WHERE (:subject IS NULL OR c.subject = :subject) AND (:grade IS NULL OR c.grade = :grade) AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND c.isPublic = true")
    List<ContentItem> searchPublic(
            @Param("subject") String subject, @Param("grade") String grade, @Param("keyword") String keyword);
}
