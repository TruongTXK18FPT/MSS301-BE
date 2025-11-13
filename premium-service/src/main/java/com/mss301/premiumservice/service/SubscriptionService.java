package com.mss301.premiumservice.service;

import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.model.UsageMeter;
import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.request.UsageMeterRequest;
import com.mss301.premiumservice.model.dtos.response.SubscriptionResponse;
import com.mss301.premiumservice.model.dtos.response.SubscriptionWithPaymentResponse;
import com.mss301.premiumservice.model.dtos.response.UsageMeterResponse;

import java.util.List;

public interface SubscriptionService {
    SubscriptionWithPaymentResponse createSubscriptionWithPayment(
            SubscriptionRequest request);

    /**
     * Create subscription with PENDING_PAYMENT status
     * Supports guardian purchase via beneficiaryEmail
     */
    SubscriptionResponse createSubscription(SubscriptionRequest request);

    List<SubscriptionResponse> findSubscriptionByUserId(Long userId);

    SubscriptionResponse findBySubscriptionId(Long id);

    /**
     * Get current active subscription for a user
     * Returns the most recent SUBSCRIBED subscription that hasn't expired
     */
    SubscriptionResponse getCurrentActiveSubscription(Long userId);

    /**
     * Get subscription history for a user (all subscriptions ordered by creation
     * date)
     */
    List<SubscriptionResponse> getSubscriptionHistory(Long userId);
}
