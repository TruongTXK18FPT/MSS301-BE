package com.mss301.contentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.contentservice.entity.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentId(Long assignmentId);

    List<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<Submission> findByStudentId(Long studentId);

    List<Submission> findByAssignmentIdAndStatus(Long assignmentId, Submission.Status status);

    @Query("SELECT s FROM Submission s WHERE s.assignmentId = :assignmentId ORDER BY s.submittedAt DESC")
    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(@Param("assignmentId") Long assignmentId);

    long countByAssignmentId(Long assignmentId);

    long countByAssignmentIdAndStatus(Long assignmentId, Submission.Status status);
}
