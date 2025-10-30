package com.mss301.premiumservice.service; 
 
import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.model.UsageMeter;
import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.request.UsageMeterRequest;
import com.mss301.premiumservice.model.dtos.response.SubscriptionResponse;
import com.mss301.premiumservice.model.dtos.response.UsageMeterResponse;

import java.util.List; 
 
public interface SubscriptionService { 
   List<SubscriptionResponse> findAllSubscription();

   SubscriptionResponse findBySubscriptionId(Long subscriptionId);
   UsageMeterResponse findByUsageMeterId(Long usageMeterId);

   SubscriptionResponse subscription(SubscriptionRequest subscription);

   SubscriptionResponse update(Long subscriptionId, SubscriptionRequest subscription);
   SubscriptionResponse delete(Long subscriptionId);

   List<SubscriptionResponse> findSubscriptionByUserId(Long userId);

}
