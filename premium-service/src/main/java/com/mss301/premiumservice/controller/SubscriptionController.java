package com.mss301.premiumservice.controller;

import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.response.SubscriptionResponse;
import com.mss301.premiumservice.model.dtos.response.SubscriptionWithPaymentResponse;
import com.mss301.premiumservice.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/premium")
@CrossOrigin
@Slf4j
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/purchase")
    public ResponseEntity<SubscriptionWithPaymentResponse> purchaseSubscription(
            @RequestBody SubscriptionRequest request) {

        log.info("Creating subscription for user: {}, plan: {}",
                request.getUserId(), request.getPlanId());

        SubscriptionWithPaymentResponse response = subscriptionService
                .createSubscriptionWithPayment(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> findBySubscriptionId(@PathVariable("subscriptionId") Long subscriptionId) {
        SubscriptionResponse subscription = subscriptionService.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            return ResponseEntity.ok(subscription);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/subscription/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> findSubscriptionByUserId(@PathVariable("userId") Long userId) {
        List<SubscriptionResponse> subscriptions = subscriptionService.findSubscriptionByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }
} 
