package com.mss301.paymentservice.model.dtos.response;

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

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private long used;

    private long limit;

    private LocalDateTime updatedAt;
}
