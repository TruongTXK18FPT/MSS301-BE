package com.mss301.documentservice.repository;

import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<Document, String> {

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByStatusOrderByUploadedAtDesc(DocumentStatus status);

    List<Document> findAllByOrderByUploadedAtDesc();

    boolean existsByFilename(String filename);
}
