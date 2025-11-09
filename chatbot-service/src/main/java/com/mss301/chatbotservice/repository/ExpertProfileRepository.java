package com.mss301.chatbotservice.repository; 
 
import com.mss301.chatbotservice.model.ExpertProfile; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List; 
 
@Repository 
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> { 
   ExpertProfile findByExpertProfileId(Long expertProfileId); 
   List<ExpertProfile> findAll(); 
} 
