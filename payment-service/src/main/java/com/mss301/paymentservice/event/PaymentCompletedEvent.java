package com.mss301.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Kafka event for payment completion
 * Consumed by premium-service to activate subscriptions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long subscriptionId;
    private Long userId;
    private Long planId;
    private Long amount;
    private String payosPaymentLinkId;
    private String payosTransactionRef;
    private LocalDateTime completedAt;
}