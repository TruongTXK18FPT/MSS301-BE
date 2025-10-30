package com.mss301.mindmapservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.ClassroomMindmap;

@Repository
public interface ClassroomMindmapRepository extends JpaRepository<ClassroomMindmap, Long> {
    
    List<ClassroomMindmap> findByClassroomIdAndIsActiveTrue(Long classroomId);
    
    List<ClassroomMindmap> findByMindmapIdAndIsActiveTrue(Long mindmapId);
    
    List<ClassroomMindmap> findByTeacherIdAndIsActiveTrue(Long teacherId);
    
    Optional<ClassroomMindmap> findByMindmapIdAndClassroomIdAndIsActiveTrue(Long mindmapId, Long classroomId);
    
    void deleteByMindmapId(Long mindmapId);
}
