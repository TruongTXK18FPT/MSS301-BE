package com.mss301.premiumservice.repository; 
 
import com.mss301.premiumservice.model.UsageMeter; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List; 
 
@Repository 
public interface UsageMeterRepository extends JpaRepository<UsageMeter, Long> { 
   UsageMeter findByUsageMeterId(Long usageMeterId); 
   List<UsageMeter> findAll();
   List<UsageMeter> findByUserId(Long userId);
} 
