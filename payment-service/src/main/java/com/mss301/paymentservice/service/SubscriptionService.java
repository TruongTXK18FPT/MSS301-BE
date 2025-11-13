package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.response.PlanResponse;
import com.mss301.paymentservice.model.dtos.response.SubscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "${service.premium}", url = "${service.url}")
public interface SubscriptionService {
    @GetMapping("/premium/premium/subscription/user/{userId}")
    ResponseEntity<List<SubscriptionResponse>> findSubscriptionByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/premium/premium/subscription/{subscriptionId}")
    ResponseEntity<SubscriptionResponse> findBySubscriptionId(@PathVariable("subscriptionId") Long subscriptionId);

//    @GetMapping("/plans/{planId}")
//    ResponseEntity<PlanResponse> findByPlanId(@PathVariable("planId") Long planId);
}
