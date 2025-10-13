package com.mss301.contentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findByOwnerId(Long ownerId);

    List<ContentItem> findByIsPublicTrue();

    List<ContentItem> findByOwnerIdAndType(Long ownerId, Type type);
}
