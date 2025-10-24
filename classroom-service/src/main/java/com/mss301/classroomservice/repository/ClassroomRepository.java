package com.mss301.classroomservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.classroomservice.entity.Classroom;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByOwnerId(Long ownerId);

    List<Classroom> findByIsPublicTrue();

    Optional<Classroom> findByJoinCode(String joinCode);
    
    List<Classroom> findByIsPublicTrueAndNameContainingIgnoreCase(String keyword);
}
