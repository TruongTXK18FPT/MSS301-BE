package com.mss301.chatbotservice.repository; 
 
import com.mss301.chatbotservice.model.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; 
 
import java.util.List;
import java.util.Optional;

@Repository 
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {
   Optional<ExpertProfile> findById(Long id);
   Optional<ExpertProfile> findByCode(String code);
   List<ExpertProfile> findAll(); 
} 
