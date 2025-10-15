package com.mss301.documentservice.repository;

import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.JobStatus;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingJobRepository extends ElasticsearchRepository<ProcessingJob, String> {

    Optional<ProcessingJob> findByDocumentId(String documentId);

    List<ProcessingJob> findByStatus(JobStatus status);

    List<ProcessingJob> findByStatusOrderByStartedAtDesc(JobStatus status);

    List<ProcessingJob> findAllByOrderByStartedAtDesc();
}