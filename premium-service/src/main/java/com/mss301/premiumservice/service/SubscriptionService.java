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

    List<SubscriptionResponse> findSubscriptionByUserId(Long userId);

    SubscriptionResponse findBySubscriptionId(Long id);
}
