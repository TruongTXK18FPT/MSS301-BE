package com.mss301.classroomservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.classroomservice.entity.AssignmentSubmission;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {}
