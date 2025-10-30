package com.mss301.premiumservice.repository; 
 
import com.mss301.premiumservice.model.Subscription; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List; 
 
@Repository 
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> { 
   Subscription findBySubscriptionId(Long subscriptionId); 
   List<Subscription> findAll();
   List<Subscription> findByUserId(Long userId);
} 
