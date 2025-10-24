package com.mss301.classroomservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.classroomservice.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    List<Assignment> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);
    
    List<Assignment> findByClassroomIdAndIsPublishedTrueOrderByDueDateAsc(Long classroomId);
    
    List<Assignment> findByClassroomId(Long classroomId);
    
    @Query("SELECT a FROM Assignment a WHERE a.classroomId = :classroomId AND a.createdBy = :teacherId ORDER BY a.createdAt DESC")
    List<Assignment> findByClassroomIdAndTeacher(Long classroomId, Long teacherId);
}
