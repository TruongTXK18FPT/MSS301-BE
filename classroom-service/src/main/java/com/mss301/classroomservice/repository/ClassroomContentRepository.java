package com.mss301.classroomservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.classroomservice.entity.ClassroomContent;

@Repository
public interface ClassroomContentRepository extends JpaRepository<ClassroomContent, Long> {
    List<ClassroomContent> findByClassroomIdOrderByOrderIndexAsc(Long classroomId);

    Optional<ClassroomContent> findByClassroomIdAndContentId(Long classroomId, Long contentId);
}
