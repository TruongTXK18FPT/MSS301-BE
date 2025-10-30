package com.mss301.paymentservice.model.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MomoResponse {
    private String status;
    private String requestId;
    private long amount;
    private String orderInfo;
    private String message;
}
