package com.mss301.paymentservice.constant;

/**
 * Payment status lifecycle:
 * PENDING -> PROCESSING -> SUCCESS
 * -> CANCELLED (user cancels)
 * -> FAILED (payment error)
 * -> EXPIRED (timeout after 15 minutes)
 */
public enum Status {
    PENDING, // Payment created, waiting for user action
    PROCESSING, // PayOS is processing the payment
    SUCCESS, // Payment completed successfully
    FAILED, // Payment failed due to error
    CANCELLED, // Payment cancelled by user
    EXPIRED // Payment link expired (15 minutes timeout)
}
