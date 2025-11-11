package com.mss301.premiumservice.model.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private Long userId;

    // Mã đơn hàng bên hệ thống của bạn
    private Long subscriptionId;

    private Long planId;
    private Long amount;

    // thông tin đơn hàng
    private String orderInfo;
}
