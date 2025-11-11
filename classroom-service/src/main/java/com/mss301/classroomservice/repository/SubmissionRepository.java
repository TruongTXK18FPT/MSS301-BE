package com.mss301.classroomservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.classroomservice.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByClassroomContentIdAndStudentId(Long classroomContentId, Long studentId);
    List<Submission> findByClassroomContentId(Long classroomContentId);
    List<Submission> findByStudentId(Long studentId);
    long countByClassroomContentId(Long classroomContentId);
}
