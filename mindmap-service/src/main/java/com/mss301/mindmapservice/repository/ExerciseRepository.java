package com.mss301.mindmapservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.Exercise;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    
    List<Exercise> findByNodeIdAndIsActiveTrue(Long nodeId);
    
    List<Exercise> findByNodeId(Long nodeId);
    
    List<Exercise> findByCreatedBy(Long userId);
    
    List<Exercise> findByNodeIdAndDifficulty(Long nodeId, Exercise.DifficultyLevel difficulty);
    
    List<Exercise> findByNodeIdAndCognitiveLevel(Long nodeId, Exercise.CognitiveLevel cognitiveLevel);
    
    void deleteByNodeId(Long nodeId);
}
