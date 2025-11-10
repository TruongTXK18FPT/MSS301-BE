package com.mss301.premiumservice.model.dtos.response;

import com.mss301.premiumservice.constant.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long subscriptionId;
    private Long userId;
    private Long amount;
    private String orderInfo;
    private Long planId;
//    private String momoRequestId;
    private String momoTransId;
    private String paymentUrl;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
