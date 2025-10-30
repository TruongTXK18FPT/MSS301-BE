package com.mss301.premiumservice.model.dtos.response;

import com.mss301.premiumservice.constant.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementResponse {

    private Long entitlementId;

    private String code;

    private String name;

    private String description;

    private long defaultLimit;

    private Unit unit;
}
