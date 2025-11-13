package com.mss301.paymentservice.model.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoRequest {

    //Định danh duy nhất cho mỗi yêu cầu
    private String requestId;

    // số tiền cần thanh toán
    private long amount;

    // Mã đơn hàng bên hệ thống của bạn
//    private Long subscriptionId;

    // Mã sản phẩm bên hệ thống của bạn
    private Long planId;

    // thông tin đơn hàng
    private String orderInfo;

    public String getPlanId() {
        return planId != null ? planId.toString() : null;
    }

    public MomoRequest(PaymentRequest paymentRequest) {
        this.requestId = UUID.randomUUID().toString();
        this.amount = paymentRequest.getAmount();
        this.planId = paymentRequest.getPlanId();
        this.orderInfo = paymentRequest.getOrderInfo();
    }
}
