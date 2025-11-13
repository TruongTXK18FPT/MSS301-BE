package com.mss301.premiumservice.listener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Kafka event for payment completion
 * Must match payment-service PaymentCompletedEvent
 */
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

    // Default constructor for deserialization
    public PaymentCompletedEvent() {
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPayosPaymentLinkId() {
        return payosPaymentLinkId;
    }

    public void setPayosPaymentLinkId(String payosPaymentLinkId) {
        this.payosPaymentLinkId = payosPaymentLinkId;
    }

    public String getPayosTransactionRef() {
        return payosTransactionRef;
    }

    public void setPayosTransactionRef(String payosTransactionRef) {
        this.payosTransactionRef = payosTransactionRef;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
