package com.mss301.documentservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.JobStatus;

@Repository
public interface ProcessingJobRepository extends ElasticsearchRepository<ProcessingJob, String> {

    Optional<ProcessingJob> findByDocumentId(String documentId);

    List<ProcessingJob> findByStatus(JobStatus status);

    List<ProcessingJob> findByStatusOrderByStartedAtDesc(JobStatus status);

    List<ProcessingJob> findAllByOrderByStartedAtDesc();
}
