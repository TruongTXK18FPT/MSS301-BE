package com.mss301.premiumservice.controller;

import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.response.SubscriptionResponse;
import com.mss301.premiumservice.model.dtos.response.UsageMeterResponse;
import com.mss301.premiumservice.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/premium")
@CrossOrigin
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> findAll() {
        return ResponseEntity.ok(subscriptionService.findAllSubscription());
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscription(@RequestBody SubscriptionRequest subscription) {
        return ResponseEntity.ok(subscriptionService.subscription(subscription));
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> findBySubscriptionId(@PathVariable("subscriptionId") Long subscriptionId) {
        SubscriptionResponse subscription = subscriptionService.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            return ResponseEntity.ok(subscription);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> update(@PathVariable Long subscriptionId, @RequestBody SubscriptionRequest subscription) {
        SubscriptionResponse updatedSubscription = subscriptionService.update(subscriptionId, subscription);
        if (updatedSubscription != null) {
            return ResponseEntity.ok(updatedSubscription);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> delete(@PathVariable Long subscriptionId) {
        SubscriptionResponse deletedSubscription = subscriptionService.delete(subscriptionId);
        if (deletedSubscription != null) {
            return ResponseEntity.ok(deletedSubscription);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/usages/{usageMeterId}")
    public ResponseEntity<UsageMeterResponse> findByUsageMeterId(@PathVariable("usageMeterId") Long usageMeterId) {
        UsageMeterResponse usageMeter = subscriptionService.findByUsageMeterId(usageMeterId);
        if (usageMeter != null) {
            return ResponseEntity.ok(usageMeter);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> findSubscriptionByUserId(@PathVariable("userId") Long userId) {
        List<SubscriptionResponse> subscriptions = subscriptionService.findSubscriptionByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }
} 
