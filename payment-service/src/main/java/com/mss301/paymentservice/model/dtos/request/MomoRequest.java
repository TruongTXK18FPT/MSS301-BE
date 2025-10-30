package com.mss301.paymentservice.model.dtos.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class MomoRequest {

    //Định danh duy nhất cho mỗi yêu cầu
    private String requestId;

    // số tiền cần thanh toán
    private long amount;

    // Mã đơn hàng bên hệ thống của bạn
    private Long subscriptionId;

    // thông tin đơn hàng
    private String orderInfo;

    public String getOrderId() {
        return subscriptionId != null ? subscriptionId.toString() : null;
    }

    public MomoRequest(PaymentRequest paymentRequest) {
        this.requestId = UUID.randomUUID().toString();
        this.amount = paymentRequest.getAmount();
        this.subscriptionId = paymentRequest.getSubscriptionId();
        this.orderInfo = paymentRequest.getOrderInfo();
    }
}
