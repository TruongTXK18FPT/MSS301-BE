package com.mss301.paymentservice.model.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Request DTO
 * Used for creating new payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    // Set by controller from JWT token
    private Long userId;

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Amount is required")
    private Long amount;

    // Optional: Order description
    private String orderInfo;
}
