package com.mss301.mindmapservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.Concept;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, Long> {
    
    List<Concept> findByNodeId(Long nodeId);
    
    List<Concept> findByNodeIdOrderByOrderIndexAsc(Long nodeId);
    
    void deleteByNodeId(Long nodeId);
}
