package com.mss301.classroomservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mss301.classroomservice.entity.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findBySubmissionId(Long submissionId);
    List<Grade> findByStudentId(Long studentId);
    
    @Query("SELECT AVG(g.points) FROM Grade g WHERE g.studentId = :studentId")
    Double getAverageScoreByStudent(Long studentId);
}
