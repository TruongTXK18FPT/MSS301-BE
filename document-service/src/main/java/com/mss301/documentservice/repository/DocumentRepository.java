package com.mss301.documentservice.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.enums.DocumentStatus;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<Document, String> {

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByStatusOrderByUploadedAtDesc(DocumentStatus status);

    List<Document> findAllByOrderByUploadedAtDesc();

    boolean existsByFileName(String filename);
}
