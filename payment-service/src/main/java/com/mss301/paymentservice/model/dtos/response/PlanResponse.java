package com.mss301.paymentservice.model.dtos.response;

import com.mss301.paymentservice.constant.Currency;
import com.mss301.paymentservice.constant.PlanStatus;
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

    private long price; // Price in VND (not cents)

    private Currency currency;

    private PlanStatus planStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
