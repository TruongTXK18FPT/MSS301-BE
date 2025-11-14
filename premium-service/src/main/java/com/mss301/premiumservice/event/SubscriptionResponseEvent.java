package com.mss301.premiumservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Kafka event for subscription response
 * Published by premium-service, consumed by payment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId; // Correlation ID matching the request
    private Long subscriptionId;
    private Long userId;
    private Long planId;
    private Long planPrice; // Price in VND
    private String subscriptionStatus;
    private Boolean success;
    private String errorMessage; // If success = false
}
