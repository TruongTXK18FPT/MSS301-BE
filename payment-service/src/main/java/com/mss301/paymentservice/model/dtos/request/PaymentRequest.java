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

    // số tiền cần thanh toán
    private long amount;

    // Mã đơn hàng bên hệ thống của bạn
    private Long subscriptionId;

    // thông tin đơn hàng
    private String orderInfo;
}
