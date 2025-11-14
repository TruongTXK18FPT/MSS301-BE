package com.mss301.premiumservice.model.dtos.request;

import com.mss301.premiumservice.constant.Currency;
import com.mss301.premiumservice.model.Entitlement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @Size(min = 1)
    private int billingCycle;

    @NotNull
    @Size(min = 1)
    private long price; // Price in VND (not cents)

    @NotNull
    private Currency currency;

    @NotNull
    private List<Long> entitlementsId;
}
