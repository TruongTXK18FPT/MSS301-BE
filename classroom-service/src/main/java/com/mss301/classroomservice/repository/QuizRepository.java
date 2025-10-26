package com.mss301.classroomservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.classroomservice.entity.Quiz;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    List<Quiz> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);
    
    List<Quiz> findByClassroomIdAndIsPublishedTrueOrderByStartTimeAsc(Long classroomId);
    
    List<Quiz> findByClassroomId(Long classroomId);
    
    @Query("SELECT q FROM Quiz q WHERE q.classroomId = :classroomId AND q.createdBy = :teacherId ORDER BY q.createdAt DESC")
    List<Quiz> findByClassroomIdAndTeacher(Long classroomId, Long teacherId);
}
