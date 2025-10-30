package com.mss301.premiumservice.repository; 
 
import com.mss301.premiumservice.model.Plan; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List; 
 
@Repository 
public interface PlanRepository extends JpaRepository<Plan, Long> { 
   Plan findByPlanId(Long planId); 
   List<Plan> findAll(); 
} 
