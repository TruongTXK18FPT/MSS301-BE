package com.mss301.premiumservice.controller;

import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.response.ApiResponse;
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
@Slf4j
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/purchase")
    public ResponseEntity<SubscriptionWithPaymentResponse> purchaseSubscription(
            @RequestBody SubscriptionRequest request) {
        // Legacy endpoint - use POST /subscriptions + POST /payment/create instead
        log.info("Creating subscription for user: {}, plan: {}",
                request.getUserId(), request.getPlanId());

        SubscriptionWithPaymentResponse response = subscriptionService
                .createSubscriptionWithPayment(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Create subscription (PENDING_PAYMENT status)
     * Use with payment-service POST /payment/create
     * Gateway rewrites /api/v1/premium/subscriptions -> /premium/subscriptions
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @RequestBody SubscriptionRequest request) {
        try {
            log.info("Creating subscription for user: {}, plan: {}, beneficiary: {}",
                    request.getUserId(), request.getPlanId(), request.getBeneficiaryEmail());

            SubscriptionResponse subscriptionResponse = subscriptionService.createSubscription(request);

            if (subscriptionResponse == null) {
                log.error("Subscription service returned null for user: {}, plan: {}",
                        request.getUserId(), request.getPlanId());
                ApiResponse<SubscriptionResponse> errorResponse = ApiResponse.<SubscriptionResponse>builder()
                        .code(500)
                        .message("Failed to create subscription: service returned null")
                        .build();
                return ResponseEntity.status(500).body(errorResponse);
            }

            ApiResponse<SubscriptionResponse> response = ApiResponse.<SubscriptionResponse>builder()
                    .code(200)
                    .message("Subscription created successfully")
                    .result(subscriptionResponse)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating subscription for user: {}, plan: {}",
                    request.getUserId(), request.getPlanId(), e);
            ApiResponse<SubscriptionResponse> errorResponse = ApiResponse.<SubscriptionResponse>builder()
                    .code(500)
                    .message("Failed to create subscription: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> findBySubscriptionId(
            @PathVariable("subscriptionId") Long subscriptionId) {
        SubscriptionResponse subscription = subscriptionService.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            return ResponseEntity.ok(subscription);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Internal endpoint - no auth required
     * Used by payment-service to validate subscription
     */
    @GetMapping("/internal/subscriptions/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionInternal(
            @PathVariable("subscriptionId") Long subscriptionId) {
        log.info("Internal request for subscription: {}", subscriptionId);
        SubscriptionResponse subscription = subscriptionService.findBySubscriptionId(subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/subscription/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> findSubscriptionByUserId(@PathVariable("userId") Long userId) {
        List<SubscriptionResponse> subscriptions = subscriptionService.findSubscriptionByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Get current active subscription for a user
     * Returns the most recent SUBSCRIBED subscription that hasn't expired
     */
    @GetMapping("/subscriptions/user/{userId}/current")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrentActiveSubscription(
            @PathVariable("userId") Long userId) {
        try {
            SubscriptionResponse subscription = subscriptionService.getCurrentActiveSubscription(userId);
            if (subscription == null) {
                ApiResponse<SubscriptionResponse> response = ApiResponse.<SubscriptionResponse>builder()
                        .code(404)
                        .message("No active subscription found")
                        .result(null)
                        .build();
                return ResponseEntity.status(404).body(response);
            }
            ApiResponse<SubscriptionResponse> response = ApiResponse.<SubscriptionResponse>builder()
                    .code(200)
                    .message("Current active subscription retrieved successfully")
                    .result(subscription)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current active subscription for user: {}", userId, e);
            ApiResponse<SubscriptionResponse> errorResponse = ApiResponse.<SubscriptionResponse>builder()
                    .code(500)
                    .message("Failed to get current subscription: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get subscription history for a user (all subscriptions ordered by creation
     * date)
     */
    @GetMapping("/subscriptions/user/{userId}/history")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getSubscriptionHistory(
            @PathVariable("userId") Long userId) {
        try {
            List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionHistory(userId);
            ApiResponse<List<SubscriptionResponse>> response = ApiResponse.<List<SubscriptionResponse>>builder()
                    .code(200)
                    .message("Subscription history retrieved successfully")
                    .result(subscriptions)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting subscription history for user: {}", userId, e);
            ApiResponse<List<SubscriptionResponse>> errorResponse = ApiResponse.<List<SubscriptionResponse>>builder()
                    .code(500)
                    .message("Failed to get subscription history: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
