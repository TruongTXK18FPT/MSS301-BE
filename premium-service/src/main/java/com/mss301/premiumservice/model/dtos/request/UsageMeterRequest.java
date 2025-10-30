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
public class UsageMeterRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long entitlementId;
}
