package com.mss301.premiumservice.constant;

public enum SubscriptionStatus {
    PENDING_PAYMENT, // Waiting for payment completion
    SUBSCRIBED, // Active subscription
    EXPIRED, // Subscription ended
    CANCELLED, // User cancelled
    TRIAL, // Trial period
    PENDING // Legacy status
}
