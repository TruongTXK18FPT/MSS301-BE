package com.mss301.premiumservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Kafka event for requesting subscription data
 * Published by payment-service, consumed by premium-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId; // Correlation ID for request-reply pattern
    private Long subscriptionId;
    private String replyTopic; // Topic to send response to
}
