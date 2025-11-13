package com.mss301.premiumservice.model.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long planId;

    @NotNull
    private boolean renewal;

    /**
     * Optional: Email of beneficiary for guardian purchases
     * If null, subscription is for current user (userId)
     * If provided, subscription is for the beneficiary (validated against
     * profile-service)
     */
    private String beneficiaryEmail;
}
