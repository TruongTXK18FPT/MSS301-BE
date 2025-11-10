package com.mss301.premiumservice.model.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionWithPaymentResponse {

    private Long subscriptionId;
    private Long planId;
    private String planName;
    private long amount;
    private String paymentUrl;
}
