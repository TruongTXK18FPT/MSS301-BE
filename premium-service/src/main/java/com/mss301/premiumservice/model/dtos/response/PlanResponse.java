package com.mss301.premiumservice.model.dtos.response;

import com.mss301.premiumservice.constant.Currency;
import com.mss301.premiumservice.constant.PlanStatus;
import com.mss301.premiumservice.model.Entitlement;
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
public class PlanResponse {

    private Long planId;

    private String code;

    private String name;

    private String description;

    private int billingCycle;

    private int priceCents;

    private Currency currency;

    private PlanStatus planStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Entitlement> entitlements;
}
