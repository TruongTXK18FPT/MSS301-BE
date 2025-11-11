package com.mss301.premiumservice.model.dtos.response;

import com.mss301.premiumservice.constant.Unit;
import com.mss301.premiumservice.model.Entitlement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMeterResponse {

    private Long usageMeterId;

    private UserResponse user;

    private Long entitlementId;
    private String entitlementName;
    private String entitlementCode;
    private long defaultLimit;
    private Unit unit;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private long used;

    private long limit;

    private LocalDateTime updatedAt;
}
