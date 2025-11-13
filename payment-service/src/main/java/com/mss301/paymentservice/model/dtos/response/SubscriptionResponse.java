package com.mss301.paymentservice.model.dtos.response;

import com.mss301.paymentservice.constant.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long subscriptionId;

    private Long planId;

    private PlanResponse plan;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private SubscriptionStatus subscriptionStatus;

    private boolean renewal;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<UsageMeterResponse> usageMeters;
}
