package com.mss301.mindmapservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.mindmapservice.entity.Formula;

@Repository
public interface FormulaRepository extends JpaRepository<Formula, Long> {
    
    List<Formula> findByNodeId(Long nodeId);
    
    List<Formula> findByNodeIdAndIsPrimaryTrue(Long nodeId);
    
    List<Formula> findByNodeIdOrderByOrderIndexAsc(Long nodeId);
    
    void deleteByNodeId(Long nodeId);
}
