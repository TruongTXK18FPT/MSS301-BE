package com.mss301.premiumservice.repository; 
 
import com.mss301.premiumservice.model.Subscription; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List;
import java.util.Optional;

@Repository 
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> { 
   Optional<Subscription> findBySubscriptionId(Long subscriptionId);
   List<Subscription> findByUserId(Long userId);
} 
