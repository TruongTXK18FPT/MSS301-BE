package com.mss301.paymentservice.model.dtos.request;

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

    // subscription là đơn hàng
//    private Long subscriptionId;

    // plan là sản phẩm, ng dùng mua plan
    private Long planId;
    private long amount;

    // thông tin đơn hàng
    private String orderInfo;
}
